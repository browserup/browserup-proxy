package net.lightbody.bmp.proxy;

import net.lightbody.bmp.l10n.MessagesUtil;
import net.lightbody.bmp.proxy.test.util.ProxyServerTest;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ErrorResponseTest extends ProxyServerTest {
    @Test
    public void testCannotResolveHost() throws IOException {
        String url = "http://www.doesntexist";

        try (CloseableHttpResponse response = getResponseFromHost(url)) {
            assertEquals("Expected 502 error due to unknown host", 502, response.getStatusLine().getStatusCode());

            String responseBody = IOUtils.toStringAndClose(response.getEntity().getContent());

            String hostNotFoundTitle = MessagesUtil.getMessage("response.dns_not_found.title");

            assertTrue("Expected \"response.dns_not_found.title\" message in body of error response", responseBody.contains(hostNotFoundTitle));
            assertTrue("Expected URL in body of error response", responseBody.contains(url));
        }
    }

    @Test
    public void testConnectionRefused() throws IOException {
        String url = "http://0.0.0.0";

        try (CloseableHttpResponse response = getResponseFromHost(url)) {
            assertEquals("Expected 502 error due to connection failure", 502, response.getStatusLine().getStatusCode());

            String responseBody = IOUtils.toStringAndClose(response.getEntity().getContent());

            String connectionFailureTitle = MessagesUtil.getMessage("response.conn_failure.title");

            assertTrue("Expected \"response.conn_failure.title\" message in body of error response", responseBody.contains(connectionFailureTitle));
            assertTrue("Expected URL in body of error response", responseBody.contains(url));
        }
    }

    @Test
    public void testConnectionTimeout() throws IOException {
        proxy.setConnectionTimeout(1);

        String url = "http://1.2.3.4";

        try (CloseableHttpResponse response = getResponseFromHost(url)) {
            assertEquals("Expected 504 error due to connection timeout", 504, response.getStatusLine().getStatusCode());

            String responseBody = IOUtils.toStringAndClose(response.getEntity().getContent());

            String networkTimeoutTitle = MessagesUtil.getMessage("response.net_timeout.title");

            assertTrue("Expected \"response.net_timeout.title\" message in body of error response", responseBody.contains(networkTimeoutTitle));
            assertTrue("Expected URL in body of error response", responseBody.contains(url));
        }
    }

    //TODO: add additional tests for other error conditions
}
