package net.lightbody.bmp.proxy.test.util;

import org.junit.After;
import org.junit.Before;

/**
 * Extend this class to gain access to a local Jetty server and a local Proxy server.
 * <p/>
 * Call getHostnameAndPort() to retrieve an string that can be used to make HTTP requests to the local server.
 */
public abstract class LocalServerTest extends ProxyServerTest {
    protected LocalServer server = new LocalServer();

    @Before
    public void startServer() throws Exception {
        server.start();
        super.startServer();
    }

    @After
    public void stopServer() throws Exception {
        super.stopServer();
        server.stop();
    }

    public int getServerPort() {
        return server.getPort();
    }

    /**
     * Returns the hostname and port of the running server, prefixed with http, without a trailing slash.
     *
     * @return http + hostname + port, e.g. http://localhost:19024
     */
    public String getHostnameAndPort() {
        return "http://127.0.0.1:" + getServerPort();
    }

}
