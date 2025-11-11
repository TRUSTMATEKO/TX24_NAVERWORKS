package kr.tx24.naverworks.oauth;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.tx24.inet.conf.INetConfigLoader;
import kr.tx24.lib.executor.AsyncExecutor;
import kr.tx24.lib.map.LinkedMap;
import kr.tx24.lib.map.SharedMap;
import kr.tx24.lib.map.TypeRegistry;
import kr.tx24.lib.mapper.JacksonUtils;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TokenManager {

	private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);
    
	private final LinkedMap<String,Object> oauthMap;
    private final JwtBuilder jwtBuilder;
    private final OkHttpClient httpClient;
    
    // 토큰 정보
    private volatile String accessToken;
    private volatile long tokenExpiresAt; // 만료 시간 (epoch millis)
    
    // Thread-safe 제어
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    public TokenManager(LinkedMap<String,Object> oauthMap) throws Exception {
        this.oauthMap 	= oauthMap;
        this.jwtBuilder = new JwtBuilder(oauthMap);
        
        // OkHttpClient 생성
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .build();
        
        
        logger.info("TokenManager initialized for service account: {}", oauthMap.getString("serviceAccount"));
    }
    
    /**
     * 초기 토큰 발급
     */
    public void initialize() {
        if (isInitialized.get()) {
            logger.debug("TokenManager already initialized");
            return;
        }
        
        try {
            requestNewToken();
            isInitialized.set(true);
            
            // 자동 갱신 스케줄링
            if (oauthMap.getBoolean("autoRefresh")) {
                scheduleAutoRefresh();
            }
            
            logger.info("TokenManager initialized successfully, token expires at: {}", 
                    new java.util.Date(tokenExpiresAt));
            
        } catch (Exception e) {
            logger.error("Failed to initialize TokenManager", e);
            throw new RuntimeException("토큰 초기화 실패", e);
        }
    }
    
    /**
     * Access Token 조회 (필요시 갱신)
     */
    public String getAccessToken() {
        if (!isInitialized.get()) {
            throw new IllegalStateException("TokenManager not initialized. Call initialize() first.");
        }
        
        // Read lock으로 토큰 조회
        lock.readLock().lock();
        try {
            // 토큰이 유효한 경우
            if (isTokenValid()) {
                return accessToken;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        // 토큰이 만료되었으면 갱신 시도
        return refreshTokenIfNeeded();
    }
    
    /**
     * 토큰 유효성 체크
     */
    private boolean isTokenValid() {
        if (accessToken == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long refreshThreshold = TimeUnit.MINUTES.toMillis(oauthMap.getLong("refreshBeforeExpireMinutes"));
        
        // 현재 시간이 (만료시간 - refreshThreshold) 보다 이전이면 유효
        return now < (tokenExpiresAt - refreshThreshold);
    }
    
    /**
     * 토큰 갱신 (필요시)
     */
    private String refreshTokenIfNeeded() {
        // 중복 갱신 방지
        if (!isRefreshing.compareAndSet(false, true)) {
            // 다른 스레드가 이미 갱신 중
            waitForRefresh();
            return accessToken;
        }
        
        lock.writeLock().lock();
        try {
            // Double-check: 다시 한 번 유효성 확인
            if (isTokenValid()) {
                return accessToken;
            }
            
            // 토큰 갱신
            logger.info("Refreshing access token...");
            requestNewToken();
            logger.info("Access token refreshed successfully");
            
            return accessToken;
            
        } catch (Exception e) {
            logger.error("Failed to refresh token", e);
            throw new RuntimeException("토큰 갱신 실패", e);
        } finally {
            isRefreshing.set(false);
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 새 토큰 요청
     */
    private void requestNewToken() throws Exception {
        // JWT 생성
        String jwt = jwtBuilder.buildJwt();
        
        // 토큰 요청
        FormBody formBody = new FormBody.Builder()
                .add("assertion"	, jwt)
                .add("grant_type"	, "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .add("client_id"	, oauthMap.getString("clientId"))
                .add("client_secret", oauthMap.getString("clientSecret"))
                .add("scope"		, oauthMap.getString("scope"))
                .build();
        
        Request request = new Request.Builder()
                .url(oauthMap.getString("tokenServer"))
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        
        // 재시도 로직
        Exception lastException = null;
        for (int i = 0; i < oauthMap.getInt("maxRetryCount"); i++) {
            try {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No body";
                        throw new Exception("Token request failed: " + response.code() + ", body: " + errorBody);
                    }
                    
                    String responseBody = response.body().string();
                    parseTokenResponse(responseBody);
                    return; // 성공
                }
            } catch (Exception e) {
                lastException = e;
                logger.warn("Token request attempt {} failed: {}", i + 1, e.getMessage());
                
                if (i < oauthMap.getInt("maxRetryCount") - 1) {
                    try {
                        Thread.sleep(1000 * (i + 1)); // 재시도 간격 증가
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Exception("Token request interrupted", ie);
                    }
                }
            }
        }
        
        throw new Exception("Token request failed after " + oauthMap.getInt("maxRetryCount") + " attempts", lastException);
    }
    
    /**
     * 토큰 응답 파싱
     */
    private void parseTokenResponse(String responseBody) {
        try {
        	
        	SharedMap<String,Object> map = new JacksonUtils().fromJson(responseBody, TypeRegistry.MAP_SHAREDMAP_OBJECT);
        	
            this.accessToken 	= map.getString("access_token");
            int expiresIn 		= map.getInt("expires_in"); 
            if (expiresIn != 0) {
                this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
            } else {
                // 기본 3600초 (1시간)
                this.tokenExpiresAt = System.currentTimeMillis() + 3600000L;
            }
            
            logger.debug("Token parsed: expires_in={} seconds", expiresIn);
            
        } catch (Exception e) {
            logger.error("Failed to parse token response: {}", responseBody, e);
            throw new RuntimeException("토큰 응답 파싱 실패", e);
        }
    }
    
    /**
     * 자동 갱신 스케줄링
     */
    private void scheduleAutoRefresh() {
        long initialDelay = calculateInitialDelay();
        long period = TimeUnit.MINUTES.toMillis(oauthMap.getLong("refreshBeforeExpireMinutes"));
        
        AsyncExecutor.scheduleAtFixedRate(
                this::autoRefreshToken,
                initialDelay,
                period,
                TimeUnit.MILLISECONDS
        );
        
        logger.info("Auto refresh scheduled: initial delay={}ms, period={}ms", initialDelay, period);
    }
    
    /**
     * 초기 지연 시간 계산
     */
    private long calculateInitialDelay() {
        long now = System.currentTimeMillis();
        long refreshThreshold = TimeUnit.MINUTES.toMillis(oauthMap.getLong("refreshBeforeExpireMinutes"));
        long refreshTime = tokenExpiresAt - refreshThreshold;
        
        return Math.max(0, refreshTime - now);
    }
    
    /**
     * 자동 갱신 태스크
     */
    private void autoRefreshToken() {
        try {
            if (!isTokenValid()) {
                logger.info("Auto refresh triggered");
                refreshTokenIfNeeded();
            }
        } catch (Exception e) {
            logger.error("Auto refresh failed", e);
        }
    }
    
    /**
     * 갱신 완료 대기
     */
    private void waitForRefresh() {
        int attempts = 0;
        while (isRefreshing.get() && attempts < 50) {
            try {
                Thread.sleep(100);
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for token refresh", e);
            }
        }
    }
    
    /**
     * 현재 토큰 정보 출력
     */
    public String getTokenInfo() {
        lock.readLock().lock();
        try {
            long now = System.currentTimeMillis();
            long remaining = tokenExpiresAt - now;
            long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
            
            return String.format("Token Info - Valid: %s, Remaining: %d minutes, Expires at: %s",
                    isTokenValid(),
                    remainingMinutes,
                    new java.util.Date(tokenExpiresAt));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 강제 갱신
     */
    public void forceRefresh() {
        logger.info("Force refresh requested");
        lock.writeLock().lock();
        try {
            requestNewToken();
            logger.info("Force refresh completed");
        } catch (Exception e) {
            logger.error("Force refresh failed", e);
            throw new RuntimeException("강제 갱신 실패", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
