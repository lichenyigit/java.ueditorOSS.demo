package ueditorOSS.exception;

public class BadHttpStatusException extends HttpRequestFailedException {
	private static final long serialVersionUID = -7112183163026865814L;
	private final int status;
	private final String content;
	public BadHttpStatusException(int status, String content) {
		super(status);
		this.status = status;
		this.content = content;
	}
	
	public int getStatus() {
		return status;
	}
	
	public String getContent() {
		return content;
	}

	@Override
	public String getErrorCode() {
		return "HU0010";
	}

}
