/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Network manipulation tests using the new interface.
 */
public class NetworkTest extends MockServerTest {
    @Test
    public void testConnectTimeout() throws IOException {
        BrowserUpProxy proxy = new BrowserUpProxyServer();
        proxy.setConnectTimeout(1, TimeUnit.SECONDS);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            long start = System.nanoTime();
            HttpResponse response = client.execute(new HttpGet("http://1.2.3.4:53540/connecttimeout"));
            long stop = System.nanoTime();

            assertEquals("Expected to receive an HTTP 502 (Bad Gateway) response after proxy could not connect within 1 second", 502, response.getStatusLine().getStatusCode());
            assertTrue("Expected connection timeout to happen after approximately 1 second. Total time was: " + TimeUnit.MILLISECONDS.convert(stop - start, TimeUnit.NANOSECONDS) + "ms",
                    TimeUnit.SECONDS.convert(stop - start, TimeUnit.NANOSECONDS) < 2);
        } finally {
            proxy.abort();
        }
    }

    @Test
    public void testIdleConnectionTimeout() throws IOException {
        String url = "/idleconnectiontimeout";
        stubFor(get(urlEqualTo(url)).willReturn(ok().withFixedDelay((int) TimeUnit.SECONDS.toMillis(5))));

        BrowserUpProxy proxy = new BrowserUpProxyServer();
        proxy.setIdleConnectionTimeout(1, TimeUnit.SECONDS);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            long start = System.nanoTime();
            HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/idleconnectiontimeout"));
            long stop = System.nanoTime();

            assertEquals("Expected to receive an HTTP 504 (Gateway Timeout) response after proxy did not receive a response within 1 second", 504, response.getStatusLine().getStatusCode());
            assertTrue("Expected idle connection timeout to happen after approximately 1 second. Total time was: " + TimeUnit.MILLISECONDS.convert(stop - start, TimeUnit.NANOSECONDS) + "ms",
                    TimeUnit.SECONDS.convert(stop - start, TimeUnit.NANOSECONDS) < 2);
        } finally {
            proxy.abort();
        }

        verify(1, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testLatency() throws IOException {
        String url = "/latency";
        stubFor(get(urlEqualTo(url)).willReturn(ok()));

        BrowserUpProxy proxy = new BrowserUpProxyServer();
        proxy.setLatency(2, TimeUnit.SECONDS);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            long start = System.nanoTime();
            HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/latency"));
            long stop = System.nanoTime();

            assertEquals("Expected to receive an HTTP 200 from the upstream server", 200, response.getStatusLine().getStatusCode());
            assertTrue("Expected latency to be at least 2 seconds. Total time was: " + TimeUnit.MILLISECONDS.convert(stop - start, TimeUnit.NANOSECONDS) + "ms",
                    TimeUnit.SECONDS.convert(stop - start, TimeUnit.NANOSECONDS) >= 2);
        } finally {
            proxy.abort();
        }

        verify(1, getRequestedFor(urlEqualTo(url)));
    }
}
