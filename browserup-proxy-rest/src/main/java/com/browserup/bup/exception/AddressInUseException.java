/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.exception;

import java.net.InetAddress;

public class AddressInUseException extends RuntimeException {

	private static final long serialVersionUID = -2548760680049910435L;
	private final int port;
	private final InetAddress address;

    public AddressInUseException(int port, InetAddress bindAddress) {
        this.port = port;
        this.address = bindAddress;
    }

    public AddressInUseException(String message, int port, InetAddress bindAddress) {
        super(message);
        this.port = port;
        this.address = bindAddress;
    }

    public AddressInUseException(String message, Throwable cause, int port, InetAddress bindAddress) {
        super(message, cause);
        this.port = port;
        this.address = bindAddress;
    }

    public AddressInUseException(Throwable cause, int port, InetAddress bindAddress) {
        super(cause);
        this.port = port;
        this.address = bindAddress;
    }

    public int getPort() {
        return port;
    }

	public InetAddress getAddress() {
		return address;
	}
        
}
