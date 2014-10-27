package net.lightbody.bmp.proxy;

public class ProxyPortsExhaustedException extends RuntimeException {
	private static final long serialVersionUID = 5365335130190989903L;

	public ProxyPortsExhaustedException() {
		super();
	}

	public ProxyPortsExhaustedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProxyPortsExhaustedException(String message) {
		super(message);
	}

	public ProxyPortsExhaustedException(Throwable cause) {
		super(cause);
	}
}
