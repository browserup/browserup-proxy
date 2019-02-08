package com.browserup.bup.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Random;
import com.browserup.bup.BrowserUpProxyServer;
import org.junit.Test;

public class ExpiringProxyTest {
    @Test
    public void testExpiredProxyStops() throws InterruptedException {
        int minPort = new Random().nextInt(50000) + 10000;

        ProxyManager proxyManager = new ProxyManager(
            minPort,
            minPort + 100,
            2);

        BrowserUpProxyServer proxy = proxyManager.create();
        int port = proxy.getPort();

        BrowserUpProxyServer retrievedProxy = proxyManager.get(port);

        assertEquals("ProxyManager did not return the expected proxy instance", proxy, retrievedProxy);

        Thread.sleep(2500);

        // explicitly create a new proxy to cause a write to the cache. cleanups happen on "every" write and "occasional" reads, so force a cleanup by writing.
        int newPort = proxyManager.create().getPort();
        proxyManager.delete(newPort);

        BrowserUpProxyServer expiredProxy = proxyManager.get(port);

        assertNull("ProxyManager did not expire proxy as expected", expiredProxy);
    }

    @Test
    public void testZeroTtlProxyDoesNotExpire() throws InterruptedException {
        int minPort = new Random().nextInt(50000) + 10000;

        ProxyManager proxyManager = new ProxyManager(
            minPort,
            minPort + 100,
            0);

        BrowserUpProxyServer proxy = proxyManager.create();
        int port = proxy.getPort();

        BrowserUpProxyServer retrievedProxy = proxyManager.get(port);

        assertEquals("ProxyManager did not return the expected proxy instance", proxy, retrievedProxy);

        Thread.sleep(2500);

        // explicitly create a new proxy to cause a write to the cache. cleanups happen on "every" write and "occasional" reads, so force a cleanup by writing.
        int newPort = proxyManager.create().getPort();
        proxyManager.delete(newPort);

        BrowserUpProxyServer nonExpiredProxy = proxyManager.get(port);

        assertEquals("ProxyManager did not return the expected proxy instance", proxy, nonExpiredProxy);
    }

}