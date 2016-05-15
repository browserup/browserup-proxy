package net.lightbody.bmp.exception;

public class ProxyPortsExhaustedException extends RuntimeException {
    private static final long serialVersionUID = -6801448612785792233L;

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
