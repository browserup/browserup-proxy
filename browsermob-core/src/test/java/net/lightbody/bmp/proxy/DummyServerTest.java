package net.lightbody.bmp.proxy;

import net.lightbody.bmp.proxy.test.util.UnitTestServer;
import org.junit.After;
import org.junit.Before;

public abstract class DummyServerTest extends ProxyServerTest {
    protected UnitTestServer dummy = new UnitTestServer(8080);

    @Before
    public void startServer() throws Exception {
        dummy.start();
        super.startServer();
    }

    @After
    public void stopServer() throws Exception {
        super.stopServer();
        dummy.stop();
    }

}
