package ueditorOSS.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CommonUtil {

	public static HttpSession getHttpSession(HttpServletRequest request){
		HttpSession session = request.getSession(false); 
		if(session == null){
			session = request.getSession(true);
		}
		session.setMaxInactiveInterval(30*60);//session的有效时长为30分钟
		return session;
	}
	
	public static void responseCORS(HttpServletResponse response){
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "x-requested-with");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");
		response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
		response.addHeader("Access-Control-Max-Age", "1800");//30 min
		response.setCharacterEncoding("UTF-8");
	}
	
}
