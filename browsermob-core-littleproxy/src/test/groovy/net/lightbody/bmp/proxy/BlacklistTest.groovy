package net.lightbody.bmp.proxy

import net.lightbody.bmp.BrowserMobProxy
import net.lightbody.bmp.BrowserMobProxyServer
import net.lightbody.bmp.proxy.test.util.MockServerTest
import net.lightbody.bmp.proxy.test.util.ProxyServerTest
import net.lightbody.bmp.proxy.util.IOUtils
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

class BlacklistTest extends MockServerTest {
    BrowserMobProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testBlacklistedRequestReturnsBlacklistStatusCode() {
        proxy = new BrowserMobProxyServer();
        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.blacklistRequests("https?://www\\.blacklisted\\.domain/.*", 405)

        ProxyServerTest.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("http://www.blacklisted.domain/someresource"))
            assertEquals("Did not receive blacklisted status code in response", 405, response.getStatusLine().getStatusCode());

            String responseBody = IOUtils.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected  blacklisted response to contain 0-length body", responseBody, isEmptyOrNullString())
        };
    }
}
