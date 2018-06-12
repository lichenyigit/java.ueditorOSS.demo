package ueditorOSS.exception.base;

import java.util.ResourceBundle;

abstract public class BaseException extends Exception {
	private static final long serialVersionUID = -6195813912488046513L;
	private static ResourceBundle rb = ResourceBundle.getBundle("errorDefine");
	private String message;
	abstract public String getErrorCode();
	public BaseException(Object... args) {
		message = String.format(rb.getString(getErrorCode()),args);
	}
	public BaseException(Throwable t,Object... args){
		this(args);
		initCause(t);
	}
	@Override
	public String getMessage() {
		return message;
	}
}
