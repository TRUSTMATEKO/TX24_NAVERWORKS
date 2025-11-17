package kr.tx24.naverworks.bot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.tx24.lib.map.LinkedMap;

public class BotMessage {

    private String botId;
    private String channelId;
    private LinkedMap<String, Object> content;
    
    public BotMessage() {
        this.content = new LinkedMap<>();
    }
    
    public String botId() {
    	return this.botId;
    }
    
    public BotMessage botId(String botId) {
        this.botId = botId;
        return this;
    }
    
    
    public String channelId() {
    	return this.channelId;
    }
    
    public BotMessage channelId(String channelId) {
        this.channelId = channelId;
        return this;
    }
    
    public LinkedMap<String, Object> getContent(){
    	return this.content;
    }
    
    public BotMessage content(LinkedMap<String, Object> content) {
        this.content = content;
        return this;
    }
    
    public BotMessage content(Map<String, Object> content) {
        this.content = new LinkedMap<>();
        this.content.putAll(content);
        return this;
    }
    
    // Fluent Content Builders
    
    
    
    /**
     * 텍스트 메시지 설정
     */
    public BotMessage text(String text) {
        this.content.put("type", "text");
        this.content.put("text", text);
        return this;
    }
    
    /**
     * 버튼 템플릿 메시지 설정
     */
    public BotMessage buttonTemplate(String text, List<Map<String, Object>> buttons) {
        this.content.put("type", "button_template");
        this.content.put("contentText", text);
        this.content.put("buttons", buttons);
        return this;
    }
    
    /**
     * 리스트 템플릿 메시지 설정
     */
    public BotMessage listTemplate(String coverText, List<Map<String, Object>> elements) {
        this.content.put("type", "list_template");
        this.content.put("coverData", Map.of("coverText", coverText));
        this.content.put("elements", elements);
        return this;
    }
    
    /**
     * 이미지 메시지 설정
     */
    public BotMessage image(String imageUrl) {
        this.content.put("type", "image");
        this.content.put("originalContentUrl", imageUrl);
        this.content.put("previewUrl", imageUrl);
        return this;
    }
    
    /**
     * 파일 메시지 설정
     */
    public BotMessage file(String fileUrl, String fileName) {
        this.content.put("type", "file");
        this.content.put("originalContentUrl", fileUrl);
        this.content.put("fileName", fileName);
        return this;
    }
    
    /**
     * Flex 메시지 설정
     */
    public BotMessage flex(String altText, Map<String, Object> contents) {
        this.content.put("type", "flex");
        this.content.put("altText", altText);
        this.content.put("contents", contents);
        return this;
    }
    
    // Static Factory Methods (기존 호환성 유지)
    
    /**
     * 텍스트 메시지 Content 생성
     */
    public static Map<String, Object> createTextContent(String text) {
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", text);
        return content;
    }
    
    /**
     * 버튼 템플릿 메시지 Content 생성
     */
    public static Map<String, Object> createButtonTemplate(String text, List<Map<String, Object>> buttons) {
        Map<String, Object> content = new HashMap<>();
        content.put("type", "button_template");
        content.put("contentText", text);
        content.put("buttons", buttons);
        return content;
    }
    
    /**
     * 리스트 템플릿 메시지 Content 생성
     */
    public static Map<String, Object> createListTemplate(String coverText, List<Map<String, Object>> elements) {
        Map<String, Object> content = new HashMap<>();
        content.put("type", "list_template");
        content.put("coverData", Map.of("coverText", coverText));
        content.put("elements", elements);
        return content;
    }
    
    /**
     * 이미지 메시지 Content 생성
     */
    public static Map<String, Object> createImageContent(String imageUrl) {
        Map<String, Object> content = new HashMap<>();
        content.put("type", "image");
        content.put("originalContentUrl", imageUrl);
        content.put("previewUrl", imageUrl);
        return content;
    }
    
    /**
     * 파일 메시지 Content 생성
     */
    public static Map<String, Object> createFileContent(String fileUrl, String fileName) {
        Map<String, Object> content = new HashMap<>();
        content.put("type", "file");
        content.put("originalContentUrl", fileUrl);
        content.put("fileName", fileName);
        return content;
    }
    
    /**
     * Flex 메시지 Content 생성
     */
    public static Map<String, Object> createFlexContent(String altText, Map<String, Object> contents) {
        Map<String, Object> content = new HashMap<>();
        content.put("type", "flex");
        content.put("altText", altText);
        content.put("contents", contents);
        return content;
    }
}