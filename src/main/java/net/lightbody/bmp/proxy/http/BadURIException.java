package net.lightbody.bmp.proxy.http;

public class BadURIException extends RuntimeException {
	private static final long serialVersionUID = 5106174610603303551L;

	public BadURIException() {
		super();
	}

	public BadURIException(String message, Throwable cause) {
		super(message, cause);
	}

	public BadURIException(Throwable cause) {
		super(cause);
	}

	public BadURIException(String message) {
        super(message);
    }
}
