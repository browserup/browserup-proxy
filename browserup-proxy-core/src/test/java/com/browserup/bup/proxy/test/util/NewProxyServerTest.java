package com.browserup.bup.proxy.test.util;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import org.junit.After;
import org.junit.Before;

/**
 * A base class that spins up and shuts down a BrowserUpProxy instance using the new interface. IT also provides mock server support via
 * {@link com.browserup.bup.proxy.test.util.MockServerTest}.
 */
public class NewProxyServerTest extends MockServerTest {
    protected BrowserUpProxy proxy;

    @Before
    public void setUpProxyServer() {
        proxy = new BrowserUpProxyServer();
        proxy.start();
    }

    @After
    public void shutDownProxyServer() {
        if (proxy != null) {
            proxy.abort();
        }
    }

}
