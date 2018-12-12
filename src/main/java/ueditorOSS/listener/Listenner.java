package ueditorOSS.listener;

import freemarker.cache.WebappTemplateLoader;
import freemarker.template.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ueditorOSS.exception.JsonConvertFailedException;
import ueditorOSS.util.JsonUtil;
import ueditorOSS.util.Result;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@WebListener
public class Listenner implements ServletContextListener {
	private static Logger logger = LogManager.getLogger(Listenner.class);
	
	private static Context rootCtx = null;
	private static Configuration templateCfg = null;
	
	@Override
	public void contextInitialized(ServletContextEvent sce){
		Context ctx;
		try {
			ctx = new InitialContext();
			rootCtx = (Context) ctx.lookup("java:/comp/env");
		} catch (NamingException e) {
			return;
		}
		
		templateCfg = new Configuration();
		templateCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		templateCfg.setDefaultEncoding("UTF-8");
		templateCfg.setTemplateLoader(new WebappTemplateLoader(sce.getServletContext(), "/template"));
		//templateCfg.setClassForTemplateLoading(this.getClass(), "/template/");  这种写法是将template放到src/main/resources路径下
		//templateCfg.addAutoImport("base", "/base/header.ftl");
		try {
			//templateCfg.setSharedVariable("contextPath", "http://localhost:8680/"+sce.getServletContext().getContextPath());
			logger.trace(getJndiString("ueditorOSS.contextPath"));
			templateCfg.setSharedVariable("contextPath", getJndiString("ueditorOSS.contextPath"));
		} catch (TemplateModelException e) {
			e.printStackTrace();
		}
		
		logger.info(String.format("ueditorOSS Initialization complete.", ""));
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
	}
	
	public static void processTemplate(String templateName, HttpServletRequest request, HttpServletResponse response){
		try {
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html");
			Template template = templateCfg.getTemplate(templateName+".ftl");
			Writer writer = response.getWriter();
			template.process(DataModel(request), writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (TemplateException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static Map<Object, Object> DataModel(HttpServletRequest request){
		Map<Object, Object> result = new HashMap<Object, Object>();
		for (Enumeration names = request.getAttributeNames(); names.hasMoreElements(); ) {
			String name = names.nextElement().toString();
			Object value = request.getAttribute(name);
			result.put(name, value);
		}
		/*try {
			logger.trace(JsonUtil.writeJson(result));
		} catch (JsonConvertFailedException e) {
			e.printStackTrace();
		}*/
		return result;
	}
	
	public static String getJndiString(String jndi,String defaultVal){
		try{
			return (String) rootCtx.lookup(jndi);
		}catch(Exception e){
			return defaultVal;
		}
	}
	
	public static String getJndiString(String jndi){
		return getJndiString(jndi, null);
	}
	
	public static String getContextPath(){
		return getJndiString("asia.tokaji.web.contextPath");
	}
	
	public static String getServiceUrl(){
		return getJndiString("asia.tokaji.serivce.baseUrl");
	}
	

	@SuppressWarnings("all")
	public static void returnJson(Object val,HttpServletResponse response){
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		String json = null;
		try {
			json = JsonUtil.writeJson(val);
		} catch (JsonConvertFailedException e) {
			logger.error(e.getMessage(),e);
			try {
				json = JsonUtil.writeJson(new Result(e));
			} catch (JsonConvertFailedException e1) {
				logger.fatal("Convert empty Result 2 json failed!",e1);
				e1.printStackTrace();
			}
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		try {
			response.getWriter().write(json);
		} catch (IOException e) {
			logger.error("Get response writer failed!",e);
		}
	}
	
}
