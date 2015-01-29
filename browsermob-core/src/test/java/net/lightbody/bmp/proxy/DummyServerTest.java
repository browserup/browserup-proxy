package net.lightbody.bmp.proxy;

import net.lightbody.bmp.proxy.test.util.UnitTestServer;
import org.junit.After;
import org.junit.Before;

public abstract class DummyServerTest extends ProxyServerTest {
    protected UnitTestServer server = new UnitTestServer();

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
