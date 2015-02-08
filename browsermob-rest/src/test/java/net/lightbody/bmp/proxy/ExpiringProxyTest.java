package net.lightbody.bmp.proxy;

import com.google.inject.Provider;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExpiringProxyTest {
    @Test
    public void testExpiredProxyStops() throws InterruptedException {
        int minPort = new Random().nextInt(50000) + 10000;

        ProxyManager proxyManager = new ProxyManager(new Provider<ProxyServer>() {
            @Override
            public ProxyServer get() {
                return new ProxyServer();
            }
        },
                minPort,
                minPort + 100,
                2);

        ProxyServer proxy = proxyManager.create(Collections.<String, String>emptyMap());
        int port = proxy.getPort();

        ProxyServer retrievedProxy = proxyManager.get(port);

        assertEquals("ProxyManager did not return the expected proxy instance", proxy, retrievedProxy);

        Thread.sleep(2500);

        // explicitly create a new proxy to cause a write to the cache. cleanups happen on "every" write and "occasional" reads, so force a cleanup by writing.
        int newPort = proxyManager.create(Collections.<String, String>emptyMap()).getPort();
        proxyManager.delete(newPort);

        ProxyServer expiredProxy = proxyManager.get(port);

        assertNull("ProxyManager did not expire proxy as expected", expiredProxy);
    }

    @Test
    public void testZeroTtlProxyDoesNotExpire() throws InterruptedException {
        int minPort = new Random().nextInt(50000) + 10000;

        ProxyManager proxyManager = new ProxyManager(new Provider<ProxyServer>() {
            @Override
            public ProxyServer get() {
                return new ProxyServer();
            }
        },
                minPort,
                minPort + 100,
                0);

        ProxyServer proxy = proxyManager.create(Collections.<String, String>emptyMap());
        int port = proxy.getPort();

        ProxyServer retrievedProxy = proxyManager.get(port);

        assertEquals("ProxyManager did not return the expected proxy instance", proxy, retrievedProxy);

        Thread.sleep(2500);

        // explicitly create a new proxy to cause a write to the cache. cleanups happen on "every" write and "occasional" reads, so force a cleanup by writing.
        int newPort = proxyManager.create(Collections.<String, String>emptyMap()).getPort();
        proxyManager.delete(newPort);

        ProxyServer nonExpiredProxy = proxyManager.get(port);

        assertEquals("ProxyManager did not return the expected proxy instance", proxy, nonExpiredProxy);
    }

}
