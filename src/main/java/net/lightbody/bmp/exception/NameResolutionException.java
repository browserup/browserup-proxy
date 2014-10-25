package net.lightbody.bmp.exception;

/**
 * Exception indicating some sort of unrecoverable name resolution error occurred.
 */
public class NameResolutionException extends RuntimeException {
	private static final long serialVersionUID = -3358213880037217337L;

	public NameResolutionException() {
	}

	public NameResolutionException(String message) {
		super(message);
	}

	public NameResolutionException(Throwable cause) {
		super(cause);
	}

	public NameResolutionException(String message, Throwable cause) {
		super(message, cause);
	}

}
