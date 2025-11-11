package kr.tx24.naverworks.bot.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.tx24.inet.conf.INetConfigLoader;
import kr.tx24.lib.map.LinkedMap;
import kr.tx24.lib.map.TypeRegistry;
import kr.tx24.lib.mapper.JacksonUtils;
import kr.tx24.naverworks.bot.BotMessage;
import kr.tx24.naverworks.oauth.TokenManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BotService {

	
	 private static final Logger logger = LoggerFactory.getLogger(BotService.class);
	 private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	 
	 
	 private final LinkedMap<String,Object> apiMap;
	 private final TokenManager tokenManager;
	 
	 private final OkHttpClient httpClient;
	 private final JacksonUtils json;
	    
	 public BotService() throws Exception{
		 this.apiMap 		= INetConfigLoader.getMap("api",TypeRegistry.MAP_LINKEDMAP_OBJECT);
		 this.tokenManager = new TokenManager(INetConfigLoader.getMap("oauth",TypeRegistry.MAP_LINKEDMAP_OBJECT));

		 this.httpClient	= new OkHttpClient.Builder()
	                	.connectTimeout(apiMap.getLong("connectTimeout", 5*1000), TimeUnit.MILLISECONDS)
	                	.readTimeout(apiMap.getLong("readTimeout", 30*1000), TimeUnit.MILLISECONDS)
	                	.build();
		 this.json = new JacksonUtils();
		 
		 this.tokenManager.initialize();
		 logger.info("BotApiService initialized");
	 }
	 
	 public String sendMessage(BotMessage message) throws Exception {
	        
	        // API URL 생성
	        String url = String.format("%s/bots/%s/channels/%s/messages",
	                apiMap.getString("baseUrl"),
	                message.botId(),
	                message.channelId());
	        
	        // Request Body 생성
	        Map<String, Object> requestBody = new HashMap<>();
	        requestBody.put("content", message.getContent());
	        String payload = json.toJson(requestBody);
	        
	        logger.debug("message to: {}", url);
	        logger.debug("request   : {}", payload);
	        
	        // HTTP 요청
	        Request request = new Request.Builder()
	                .url(url)
	                .post(RequestBody.create(payload, JSON))
	                .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
	                .addHeader("Content-Type", "application/json")
	                .build();
	        
	        try (Response response = httpClient.newCall(request).execute()) {
	            String responseBody = response.body() != null ? response.body().string() : "";
	            
	            if (!response.isSuccessful()) {
	                logger.warn("Message send failed: code={}, body={}", response.code(), responseBody);
	                throw new Exception("메시지 전송 실패: " + response.code() + ", " + responseBody);
	            }
	            
	            logger.info("response : {}", responseBody);
	            return responseBody;
	        }
	    }
	 
	 
	 
	 
	 
}
