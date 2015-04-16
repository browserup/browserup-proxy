package net.lightbody.bmp.proxy.test.util;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.BrowserMobProxy;
import org.junit.After;
import org.junit.Before;

/**
 * A base class that spins up and shuts down a BrowserMobProxy instance using the new interface. IT also provides mock server support via
 * {@link net.lightbody.bmp.proxy.test.util.MockServerTest}.
 */
public class NewProxyServerTest extends MockServerTest {
    protected BrowserMobProxy proxy;

    @Before
    public void setUpProxyServer() {
        proxy = new BrowserMobProxyServer();
        proxy.start();
    }

    @After
    public void shutDownProxyServer() {
        if (proxy != null) {
            proxy.abort();
        }
    }
}
