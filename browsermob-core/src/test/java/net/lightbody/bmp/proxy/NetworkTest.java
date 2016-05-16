package net.lightbody.bmp.proxy;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.proxy.test.util.MockServerTest;
import net.lightbody.bmp.proxy.test.util.NewProxyServerTestUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.Delay;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Network manipulation tests using the new interface.
 */
public class NetworkTest extends MockServerTest {
    @Test
    public void testConnectTimeout() throws IOException {
        BrowserMobProxy proxy = new BrowserMobProxyServer();
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
        mockServer.when(
                request().withMethod("GET")
                        .withPath("/idleconnectiontimeout"),
                Times.exactly(1)
        ).respond(response().withDelay(new Delay(TimeUnit.SECONDS, 5)));

        BrowserMobProxy proxy = new BrowserMobProxyServer();
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
    }

    @Test
    public void testLatency() throws IOException {
        mockServer.when(
                request().withMethod("GET")
                        .withPath("/latency"),
                Times.exactly(1)
        ).respond(response().withStatusCode(200));

        BrowserMobProxy proxy = new BrowserMobProxyServer();
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
    }
}
