package kr.tx24.naverworks.oauth;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Jwts;
import kr.tx24.lib.map.LinkedMap;

public class JwtBuilder {

	private static final Logger logger = LoggerFactory.getLogger(JwtBuilder.class);
    
    private static final long JWT_EXPIRATION_MS = 3600000; // 1시간
    
    private final LinkedMap<String,Object> oauthMap;
    private final PrivateKey privateKey;
    
    public JwtBuilder(LinkedMap<String,Object> oauthMap) throws Exception {
        this.oauthMap = oauthMap;
        @SuppressWarnings("unchecked")
        List<String> privateKeyLines = (List<String>) oauthMap.get("privateKey");
        String privateKeyPem = String.join("\n", privateKeyLines);
        this.privateKey = parsePrivateKey(privateKeyPem);
    }
    
    /**
     * JWT 생성
     */
    public String buildJwt() {
        try {
            long nowMillis = System.currentTimeMillis();
            Date now = new Date(nowMillis);
            Date expiration = new Date(nowMillis + JWT_EXPIRATION_MS);
            
            String jwt = Jwts.builder()
                    .issuer(oauthMap.getString("clientId"))
                    .subject(oauthMap.getString("serviceAccount"))
                    .issuedAt(now)
                    .expiration(expiration)
                    .signWith(privateKey)
                    .compact();
            
            
            
            logger.debug("JWT created successfully");
            return jwt;
            
        } catch (Exception e) {
            logger.error("Failed to build JWT", e);
            throw new RuntimeException("JWT 생성 실패", e);
        }
    }
    
    

    
    /**
     * Private Key 파싱
     */
    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        try {
            // PEM 헤더/푸터 제거 및 개행 제거
            String privateKeyContent = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            
            // Base64 디코딩
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            
            // PKCS8 형식으로 변환
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            return keyFactory.generatePrivate(keySpec);
            
        } catch (Exception e) {
            logger.error("Failed to parse private key", e);
            throw new Exception("Private Key 파싱 실패", e);
        }
    }
}
