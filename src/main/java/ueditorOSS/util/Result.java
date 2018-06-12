package ueditorOSS.util;


import ueditorOSS.exception.base.BaseException;

public class Result<T> {
	
	private String errorCode;
	private String errorDescription;
	private Object result;
	
	public Result() {
		setErrorCode("0");
		setErrorDescription("success");
	}
	public Result(BaseException e) {
		setErrorCode(e.getErrorCode());
		setErrorDescription(e.getMessage());
	}
	public String getErrorCode() {
		return errorCode;
	}
	public String getErrorDescription() {
		return errorDescription;
	}
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
}
