package kr.tx24.naverworks.ctl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import kr.tx24.inet.mapper.Autowired;
import kr.tx24.inet.mapper.Controller;
import kr.tx24.inet.mapper.Route;
import kr.tx24.lib.inter.INet;
import kr.tx24.lib.lang.CommonUtils;
import kr.tx24.naverworks.bot.BotMessage;
import kr.tx24.naverworks.bot.service.BotService;


/**
 * 
// ===== 1. 텍스트 메시지 =====
{
    "botId": "bot123",
    "channelId": "channel456",
    "type": "text",
    "text": "안녕하세요! 텍스트 메시지입니다."
}

// ===== 2. 버튼 템플릿 메시지 =====
{
    "botId": "bot123",
    "channelId": "channel456",
    "type": "button_template",
    "contentText": "아래 버튼을 선택해주세요",
    "buttons": [
        {
            "type": "uri",
            "label": "홈페이지",
            "uri": "https://example.com"
        },
        {
            "type": "message",
            "label": "문의하기",
            "text": "문의"
        }
    ]
}

// ===== 3. 리스트 템플릿 메시지 =====
{
    "botId": "bot123",
    "channelId": "channel456",
    "type": "list_template",
    "coverText": "추천 항목 목록",
    "elements": [
        {
            "title": "항목 1",
            "subtitle": "첫 번째 항목 설명",
            "imageUrl": "https://example.com/img1.jpg"
        },
        {
            "title": "항목 2",
            "subtitle": "두 번째 항목 설명",
            "imageUrl": "https://example.com/img2.jpg"
        }
    ]
}

// ===== 4. 이미지 메시지 =====
{
    "botId": "bot123",
    "channelId": "channel456",
    "type": "image",
    "imageUrl": "https://example.com/images/sample.jpg"
}

// ===== 5. 파일 메시지 =====
{
    "botId": "bot123",
    "channelId": "channel456",
    "type": "file",
    "fileUrl": "https://example.com/files/document.pdf",
    "fileName": "문서.pdf"
}

// ===== 6. Flex 메시지 =====
{
    "botId": "bot123",
    "channelId": "channel456",
    "type": "flex",
    "altText": "Flex 메시지 대체 텍스트",
    "contents": {
        "type": "bubble",
        "body": {
            "type": "box",
            "layout": "vertical",
            "contents": [
                {
                    "type": "text",
                    "text": "Flex 메시지 제목",
                    "weight": "bold",
                    "size": "xl"
                },
                {
                    "type": "text",
                    "text": "Flex 메시지 내용",
                    "size": "sm",
                    "color": "#999999"
                }
            ]
        }
    }
}
 */

@Controller(target="/bot")
public class BotCtl {

	private static final Logger logger = LoggerFactory.getLogger(BotCtl.class);
	
	private final ChannelHandlerContext ctx;
    private final INet inet;
    private final BotService botService;
    private final INet resInet;
    
    
    @Autowired
    public BotCtl(ChannelHandlerContext ctx, INet inet) {
    	this.ctx = ctx;
    	this.inet = inet;
    	
    	BotService svc = null;
    	try{
    		svc = new BotService();
    	}catch(Exception e) {
    		logger.error("Failed to Initialize Bot Service : {}",CommonUtils.getExceptionMessage(e));
    	}
    	this.botService = svc;
    	this.resInet 	= new INet()
		        .head("result"  , true)
		        .head("message" , "successful");
    }
    
    
    @SuppressWarnings("unchecked")
	@Route(target = {"/send"}, loggable = true)
    public INet send() {
    	
    	
    	
    	if(inet.data().isEmpty("botId")) {
    		this.resInet
    			.data("resultCd", "INVALID_PARAMETER")
    			.data("resultMsg", "botId is empty");
    	}
    	
    	
    	
    	if(inet.data().isEmpty("channelId")) {
    		this.resInet
    			.data("resultCd", "INVALID_PARAMETER")
    			.data("resultMsg", "channelId is empty");
    	}
    	
    	
    	if(inet.data().isEmpty("type")) {
    		this.resInet
    			.data("resultCd", "INVALID_PARAMETER")
    			.data("resultMsg", "type is empty");
    	}
    	
    	
    	try {
    	
	    	BotMessage message = new BotMessage()
	    			.botId(inet.data().getString("botId"))
	    			.channelId(inet.data().getString("channelId"));
	    	
	    	String type = inet.data().getString("type");
	    	
	    	// type에 따른 메시지 content 설정
	        switch(type) {
	            case "text":
	                // 텍스트 메시지
	                if(inet.data().isEmpty("text")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "text is empty");
	                }
	                message.text(inet.data().getString("text"));
	                break;
	
	            case "button_template":
	                // 버튼 템플릿 메시지
	                if(inet.data().isEmpty("contentText")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "contentText is empty");
	                }
	                if(inet.data().isEmpty("buttons")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "buttons is empty");
	                }
	                message.buttonTemplate(
	                    inet.data().getString("contentText"),
	                    (List<Map<String, Object>>)inet.data().get("buttons")
	                );
	                break;
	
	            case "list_template":
	                // 리스트 템플릿 메시지
	                if(inet.data().isEmpty("coverText")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "coverText is empty");
	                }
	                if(inet.data().isEmpty("elements")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "elements is empty");
	                }
	                message.listTemplate(
	                    inet.data().getString("coverText"),
	                    (List<Map<String, Object>>)inet.data().get("elements")
	                );
	                break;
	
	            case "image":
	                // 이미지 메시지
	                if(inet.data().isEmpty("imageUrl")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "imageUrl is empty");
	                }
	                message.image(inet.data().getString("imageUrl"));
	                break;
	
	            case "file":
	                // 파일 메시지
	                if(inet.data().isEmpty("fileUrl")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "fileUrl is empty");
	                }
	                if(inet.data().isEmpty("fileName")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "fileName is empty");
	                }
	                message.file(
	                    inet.data().getString("fileUrl"),
	                    inet.data().getString("fileName")
	                );
	                break;
	
	            case "flex":
	                // Flex 메시지
	                if(inet.data().isEmpty("altText")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "altText is empty");
	                }
	                if(inet.data().isEmpty("contents")) {
	                    return this.resInet
	                        .data("resultCd", "INVALID_PARAMETER")
	                        .data("resultMsg", "contents is empty");
	                }
	                message.flex(
	                    inet.data().getString("altText"),
	                    (Map<String, Object>)inet.data().get("contents")
	                );
	                break;
	
	            default:
	                return this.resInet
	                    .data("resultCd", "INVALID_PARAMETER")
	                    .data("resultMsg", "Invalid type: " + type);
	        }
	    	
	    	
	        
	        this.botService.sendMessage(message);
	
		        
            return this.resInet
                .data("resultCd", "SUCCESS")
                .data("resultMsg", "Message sent successfully");
	        
    	}catch(Exception e) {
            logger.warn("Error sending bot message", e);
            return this.resInet
                .data("resultCd", "ERROR")
                .data("resultMsg", "Error: " + e.getMessage());
        }
        
    }
    
    
    
}
