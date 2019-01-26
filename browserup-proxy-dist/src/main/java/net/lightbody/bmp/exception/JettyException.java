package net.lightbody.bmp.exception;

/**
 * A wrapper for exceptions coming from Jetty methods that throw Exception,
 * rather than a useful Exception subclass.
 */
public class JettyException extends RuntimeException {
	private static final long serialVersionUID = 8125833642102189196L;

	public JettyException() {
	}

	public JettyException(String message) {
		super(message);
	}

	public JettyException(Throwable cause) {
		super(cause);
	}

	public JettyException(String message, Throwable cause) {
		super(message, cause);
	}

}
