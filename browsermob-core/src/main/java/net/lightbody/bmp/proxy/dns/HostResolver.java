package net.lightbody.bmp.proxy.dns;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Defines the basic functionality that {@link net.lightbody.bmp.BrowserMobProxy} implementations require when resolving hostnames.
 *
 * TODO: consider replacing this with {@link org.littleshoot.proxy.HostResolver}, which is identical.
 */
public interface HostResolver {
    /**
     * Resolves a hostname and port to an InetSocketAddress.
     *
     * @param host host to resolve
     * @param port port to resolve
     * @return resolved InetSocketAddress
     * @throws java.net.UnknownHostException if the host could not be resolved to an address
     */
    public InetSocketAddress resolve(String host, int port) throws UnknownHostException;
}
