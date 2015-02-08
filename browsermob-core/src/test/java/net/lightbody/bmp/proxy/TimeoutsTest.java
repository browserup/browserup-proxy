package net.lightbody.bmp.proxy;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.proxy.test.util.ProxyServerTest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TimeoutsTest extends ProxyServerTest {
	@Test
	public void testSmallTimeout() throws IllegalStateException, IOException {
		proxy.setRequestTimeout(2000);

        try (CloseableHttpResponse response = getResponseFromHost("http://blackhole.webpagetest.org/test")) {
            assertEquals("Expected HTTP 502 response due to timeout", 502, response.getStatusLine().getStatusCode());
        }
	}

    // tests that getHar() will time out and eventually return, even if network traffic has not stopped
    @Test
    public void testHarWaitTimeout() throws InterruptedException {
        proxy.setCaptureContent(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getResponseBodyFromHost("http://blackhole.webpagetest.org/test");
                } catch (RuntimeException e) {
                    // this will definitely throw an exception when the getHar() call times out. just eat the exception.
                }
            }
        }).start();

        // give the other thread a second to make the HTTP request
        Thread.sleep(1000);

        Har har = proxy.getHar();

        if (har != null) {
            HarLog log = har.getLog();
            if (log != null) {
                List<HarEntry> entries = log.getEntries();
                if (entries != null) {
                    assertEquals("Expected HAR to contain 0 entries because HTTP call did not complete", 0, entries.size());
                }
            }
        }
    }
}
