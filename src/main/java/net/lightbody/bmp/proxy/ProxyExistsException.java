package net.lightbody.bmp.proxy;

public class ProxyExistsException extends RuntimeException{
	private static final long serialVersionUID = 7034091787187107686L;
	
	private final int port;

    public ProxyExistsException(int port) {
        this.port = port;
    }      

    public int getPort() {
        return port;
    }
        
}
