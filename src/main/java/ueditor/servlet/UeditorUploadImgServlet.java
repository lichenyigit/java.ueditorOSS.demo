package ueditor.servlet;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ueditor.ActionEnter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns="/manager/UeditorUploadImgServlet")
public class UeditorUploadImgServlet extends HttpServlet {
	Logger logger = LogManager.getLogger(getClass());
	private static final long serialVersionUID = 5522372203700422672L;
	private String configJsonPath = "";
	String rootPath = "";
	
	@Override
	public void init() throws ServletException {
		super.init();
		configJsonPath = this.getServletContext().getRealPath("/")+"/static/UEditor/config.json";
		rootPath = this.getServletConfig().getServletContext().getRealPath("/");
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding( "utf-8" );
		response.setHeader("Content-Type" , "text/html");
		ActionEnter actionNew = new ActionEnter( request, rootPath, configJsonPath);
		logger.info(JSONObject.toJSONString(actionNew));
		response.getWriter().write(actionNew.exec());
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		request.setCharacterEncoding( "utf-8" );
		response.setHeader("Content-Type" , "text/html");
        try { 	
    		ActionEnter actionNew = new ActionEnter(request, rootPath, configJsonPath);
    		logger.info(JSONObject.toJSONString(actionNew));
    		String result = actionNew.exec();
    		logger.info("图片上传成功");
    		logger.info(result);
    		response.getWriter().write(result);
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }
    }
	
}
