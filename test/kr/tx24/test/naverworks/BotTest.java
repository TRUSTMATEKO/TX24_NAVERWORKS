package kr.tx24.test.naverworks;

import kr.tx24.lib.inter.INet;
import kr.tx24.lib.inter.INet.INMessage;

public class BotTest {
	
	
	public static void main(String[] args) {

		INet inet = new INet("NAVERWORKS")
			.head("target"		, "/bot/send")
			.data("botId"		, "11085652")
			.data("channelId"	, "02155ff2-c722-134b-f3b1-04c8d74f6cb5")
			.data("type"		, "text")
			.data("text"		, "⚠️ 시스템 오류가 발생했습니다.\n\n에러코드: E5001\n메시지: 데이터베이스 연결 실패\n\n시스템 관리자에게 문의하세요.");
		
		System.out.println("0");
		INMessage exc = inet.connect("112.175.48.148", 10010);
		
		System.out.println("1");
		if(!exc.getHead().isTrue("result")){
			
			return; 
		}else{
			
		}
		
	}
	
}
