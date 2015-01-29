package net.lightbody.bmp.proxy;

import org.junit.After;
import org.junit.Before;

public abstract class DummyServerTest extends ProxyServerTest {
    protected DummyServer dummy = new DummyServer(8080);

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
