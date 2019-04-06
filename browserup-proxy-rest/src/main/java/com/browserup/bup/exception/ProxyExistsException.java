/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.exception;

public class ProxyExistsException extends RuntimeException {
    private static final long serialVersionUID = -5515796684778166504L;

    private final int port;

    public ProxyExistsException(int port) {
        this.port = port;
    }

    public ProxyExistsException(String message, int port) {
        super(message);
        this.port = port;
    }

    public ProxyExistsException(String message, Throwable cause, int port) {
        super(message, cause);
        this.port = port;
    }

    public ProxyExistsException(Throwable cause, int port) {
        super(cause);
        this.port = port;
    }

    public int getPort() {
        return port;
    }
        
}
