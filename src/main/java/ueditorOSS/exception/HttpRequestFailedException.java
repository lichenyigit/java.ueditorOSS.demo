package ueditorOSS.exception;


import ueditorOSS.exception.base.BaseException;

public class HttpRequestFailedException extends BaseException {
	private static final long serialVersionUID = 1597920568895000516L;
	protected HttpRequestFailedException(Object... args) {
		super(args);
	}
	protected HttpRequestFailedException(Throwable t, Object... args) {
		super(t, args);
	}
	public HttpRequestFailedException(Exception e) {
		super(e);
	}
	@Override
	public String getErrorCode() {
		return "HU0020";
	}

}