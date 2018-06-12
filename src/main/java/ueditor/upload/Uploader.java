package ueditor.upload;

import ueditor.define.State;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class Uploader {
	private HttpServletRequest request = null;
	private Map<String, Object> conf = null;
	private String rootPath = null;

	public Uploader(HttpServletRequest request, Map<String, Object> conf, String rootPath) {
		this.request = request;
		this.conf = conf;
		this.rootPath = rootPath;
	}

	public final State doExec() {
		String filedName = (String) this.conf.get("fieldName");
		State state = null;

		if ("true".equals(this.conf.get("isBase64"))) {
			state = Base64Uploader.save(this.request.getParameter(filedName),
					this.conf);
		} else {
			state = BinaryUploader.save(this.request, this.conf, this.rootPath);
		}

		return state;
	}
}
