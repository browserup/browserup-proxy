package com.browserup.bup.proxy;

import com.browserup.bup.proxy.test.util.NewProxyServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QuiescenceTest extends NewProxyServerTest {
    private static final Logger log = LoggerFactory.getLogger(QuiescenceTest.class);

    @Test
    public void testWaitForQuiescenceSuccessful() throws InterruptedException {
        String url = "/quiescencesuccessful";

        stubFor(get(urlEqualTo(url)).willReturn(ok().withFixedDelay((int) TimeUnit.SECONDS.toMillis(4))));

        final AtomicLong requestComplete = new AtomicLong();

        new Thread(() -> {
            try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/quiescencesuccessful"));
                EntityUtils.consumeQuietly(response.getEntity());

                requestComplete.set(System.nanoTime());

                assertEquals("Expected successful response from server", 200, response.getStatusLine().getStatusCode());
            } catch (IOException e) {
                fail("Error occurred while connecting to server");
                log.error("Error occurred while connecting to server", e);
            }
        }).start();

        // wait for the request to start before waiting for quiescence
        Thread.sleep(1000);

        boolean waitSuccessful = proxy.waitForQuiescence(2, 10, TimeUnit.SECONDS);

        long waitForQuiescenceFinished = System.nanoTime();

        assertTrue("Expected to successfully wait for quiescence", waitSuccessful);
        assertTrue("Expected request to be complete after waiting for quiescence", requestComplete.get() > 0);

        // the total wait time after the request is complete should be approximately 2 seconds
        long wait = TimeUnit.MILLISECONDS.convert(waitForQuiescenceFinished - requestComplete.get(), TimeUnit.NANOSECONDS);

        assertTrue("Expected time to wait for quiescence to be approximately 2s. Waited for: " + wait + "ms", wait < 3000);

        verify(1, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testWaitForQuiescenceUnsuccessful() throws IOException, InterruptedException {
        String url = "/quiescenceunsuccessful";

        stubFor(get(urlEqualTo(url)).willReturn(ok().withFixedDelay((int) TimeUnit.MINUTES.toMillis(1))));

        final AtomicBoolean requestCompleted = new AtomicBoolean(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                    client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/quiescenceunsuccessful"));

                    requestCompleted.set(true);
                } catch (IOException e) {
                    // ignore any exceptions -- we don't expect this call to complete
                }
            }
        }).start();

        // wait for the request to start before waiting for quiescence
        Thread.sleep(1000);

        boolean waitSuccessful = proxy.waitForQuiescence(1, 3, TimeUnit.SECONDS);

        assertFalse("Expected waitForQuiescence to time out while waiting for traffic to stop", waitSuccessful);

        assertFalse("Did not expect request to complete", requestCompleted.get());

        verify(1, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testWaitForQuiescenceAfterRequestCompleted() throws IOException {
        String url = "/quiescencecompleted";

        stubFor(get(urlEqualTo(url)).willReturn(ok()));

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/quiescencecompleted"));
            EntityUtils.consumeQuietly(response.getEntity());

            assertEquals("Expected successful response from server", 200, response.getStatusLine().getStatusCode());
        }

        // wait for 2s of quiescence, now that the call has already completed
        long start = System.nanoTime();
        boolean waitSuccessful = proxy.waitForQuiescence(2, 5, TimeUnit.SECONDS);
        long finish = System.nanoTime();

        assertTrue("Expected to successfully wait for quiescence", waitSuccessful);

        assertTrue("Expected to wait for quiescence for approximately 2s. Actual wait time was: " + TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) + "ms",
                TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) >= 1500 && TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) <= 2500);

        verify(1, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testWaitForQuiescenceQuietPeriodAlreadySatisfied() throws IOException, InterruptedException {
        String url = "/quiescencesatisfied";

        stubFor(get(urlEqualTo(url)).willReturn(ok()));

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/quiescencesatisfied"));
            EntityUtils.consumeQuietly(response.getEntity());

            assertEquals("Expected successful response from server", 200, response.getStatusLine().getStatusCode());
        }

        // wait for 2s, then wait for 1s of quiescence, which should already be satisfied
        Thread.sleep(2000);

        long start = System.nanoTime();
        boolean waitSuccessful = proxy.waitForQuiescence(1, 5, TimeUnit.SECONDS);
        long finish = System.nanoTime();

        assertTrue("Expected to successfully wait for quiescence", waitSuccessful);

        assertTrue("Expected wait for quiescence to return immediately. Actual wait time was: " + TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) + "ms",
                TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) <= 1);

        verify(1, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testWaitForQuiescenceTimeoutLessThanQuietPeriodSuccessful() throws IOException, InterruptedException {
        String url = "/quiescencesmalltimeoutsuccess";

        stubFor(get(urlEqualTo(url)).willReturn(ok()));

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/quiescencesmalltimeoutsuccess"));
            EntityUtils.consumeQuietly(response.getEntity());

            assertEquals("Expected successful response from server", 200, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(2500);

        // wait for 3s of quiescence, which should wait no more than 500ms

        long start = System.nanoTime();
        boolean waitSuccessful = proxy.waitForQuiescence(3, 1, TimeUnit.SECONDS);
        long finish = System.nanoTime();

        assertTrue("Expected to successfully wait for quiescence", waitSuccessful);

        assertTrue("Expected to wait for quiescence for approximately 500ms. Actual wait time was: " + TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) + "ms",
                TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) >= 300 && TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) <= 700);

        verify(1, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testWaitForQuiescenceTimeoutLessThanQuietPeriodUnuccessful() throws IOException, InterruptedException {
        String url = "/quiescencesmalltimeoutunsuccessful";

        stubFor(get(urlEqualTo(url)).willReturn(ok()));

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/quiescencesmalltimeoutunsuccessful"));
            EntityUtils.consumeQuietly(response.getEntity());

            assertEquals("Expected successful response from server", 200, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(1000);

        // wait for 3s of quiescence within 1s, which should not be possible since the last request just finished. waitForQuiescence should
        // be able to detect that and return immediately.
        long start = System.nanoTime();
        boolean waitSuccessful = proxy.waitForQuiescence(3, 1, TimeUnit.SECONDS);
        long finish = System.nanoTime();

        assertFalse("Expected to unsuccessfully wait for quiescence", waitSuccessful);

        assertTrue("Expected wait for quiescence to return immediately. Actual wait time was: " + TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) + "ms",
                TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) >= 0 && TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) <= 10);

        verify(1, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testWaitForQuiescenceInterruptedBySecondRequestSuccessful() throws InterruptedException {
        String url = "/successquiesence2s";

        stubFor(get(urlEqualTo(url)).willReturn(ok().withFixedDelay((int) TimeUnit.SECONDS.toMillis(2))));

        final AtomicLong secondRequestFinished = new AtomicLong();

        final AtomicInteger firstRequestStatusCode = new AtomicInteger();
        final AtomicInteger secondRequestStatusCode = new AtomicInteger();

        final AtomicBoolean exceptionOccurred = new AtomicBoolean();

        new Thread(() -> {
            try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/successquiesence2s"));
                EntityUtils.consumeQuietly(response.getEntity());
                firstRequestStatusCode.set(response.getStatusLine().getStatusCode());

                Thread.sleep(1000);

                response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/successquiesence2s"));
                EntityUtils.consumeQuietly(response.getEntity());

                secondRequestFinished.set(System.nanoTime());

                secondRequestStatusCode.set(response.getStatusLine().getStatusCode());
            } catch (IOException | InterruptedException e) {
                exceptionOccurred.set(true);

                log.error("Exception occurred while making HTTP request", e);
            }
        }).start();

        // wait for the request to start before waiting for quiescence
        Thread.sleep(1000);

        long start = System.nanoTime();
        boolean waitSuccessful = proxy.waitForQuiescence(2, 10, TimeUnit.SECONDS);
        long finish = System.nanoTime();

        assertFalse("An exception occurred while making an HTTP request", exceptionOccurred.get());
        assertEquals("Expected successful response from server on first request", 200, firstRequestStatusCode.get());
        assertTrue("Expected second request to be finished", secondRequestFinished.get() > 0);
        assertEquals("Expected successful response from server on second request", 200, secondRequestStatusCode.get());

        assertTrue("Expected to successfully wait for quiescence", waitSuccessful);

        assertTrue("Expected waitForQuiescence to return after approximately 2s of quiescence. Actual time: "
                        + TimeUnit.MILLISECONDS.convert(finish - secondRequestFinished.get(), TimeUnit.NANOSECONDS) + "ms",
                TimeUnit.MILLISECONDS.convert(finish - secondRequestFinished.get(), TimeUnit.NANOSECONDS) >= 1600
                        && TimeUnit.MILLISECONDS.convert(finish - secondRequestFinished.get(), TimeUnit.NANOSECONDS) <= 2400);

        assertTrue("Expected to wait for quiescence for approximately 6s. Actual wait time was: " + TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) + "ms",
                TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) >= 5000 && TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) <= 7000);

        verify(2, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testWaitForQuiescenceInterruptedBySecondRequestUnsuccessful() throws InterruptedException {
        String url1 = "/quiesence2s";
        String url2 = "/quiesence5s";
        stubFor(get(urlEqualTo(url1)).willReturn(ok().withFixedDelay((int) TimeUnit.SECONDS.toMillis(2))));
        stubFor(get(urlEqualTo(url2)).willReturn(ok().withFixedDelay((int) TimeUnit.SECONDS.toMillis(5))));

        final AtomicInteger firstResponseStatusCode = new AtomicInteger();
        final AtomicBoolean secondRequestCompleted = new AtomicBoolean(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                    HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/quiesence2s"));
                    EntityUtils.consumeQuietly(response.getEntity());

                    firstResponseStatusCode.set(response.getStatusLine().getStatusCode());

                    Thread.sleep(1000);

                    response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/quiesence5s"));
                    EntityUtils.consumeQuietly(response.getEntity());

                    secondRequestCompleted.set(true);
                } catch (IOException | InterruptedException e) {
                }
            }
        }).start();

        // wait for the request to start before waiting for quiescence
        Thread.sleep(1000);

        // waitForQuiescence should exit after 3s, since it will not be possible to satisfy the 2s quietPeriod while a request is in progress at 3s
        long start = System.nanoTime();
        boolean waitSuccessful = proxy.waitForQuiescence(2, 5, TimeUnit.SECONDS);
        long finish = System.nanoTime();

        assertFalse("Expected to unsuccessfully wait for quiescence", waitSuccessful);

        assertTrue("Expected to wait for quiescence for approximately 3s. Actual wait time was: " + TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) + "ms",
                TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) >= 2500 && TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS) <= 3500);

        assertEquals("Expected successful response from server on first request", 200, firstResponseStatusCode.get());
        assertFalse("Did not expect second request to complete", secondRequestCompleted.get());
    }
}
