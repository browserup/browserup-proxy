package net.lightbody.bmp.proxy;

public class ProxyExistsException extends Exception{
    private final int port;

    public ProxyExistsException(int port) {
        this.port = port;
    }      

    public int getPort() {
        return port;
    }
        
}
