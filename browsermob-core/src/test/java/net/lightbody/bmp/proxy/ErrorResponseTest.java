package net.lightbody.bmp.proxy;

import net.lightbody.bmp.l10n.MessagesUtil;
import net.lightbody.bmp.proxy.test.util.ProxyServerTest;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class ErrorResponseTest extends ProxyServerTest {
    @Test
    public void testCannotResolveHost() throws IOException {
        String url = "http://www.doesntexist";

        try (CloseableHttpResponse response = getResponseFromHost(url)) {
            assertEquals("Expected 502 error due to unknown host", 502, response.getStatusLine().getStatusCode());

            //TODO: determine if it is possible, or desirable, to modify the error response body when using LittleProxy
            if (!Boolean.getBoolean("bmp.use.littleproxy")) {
                String responseBody = IOUtils.toStringAndClose(response.getEntity().getContent());

                String hostNotFoundTitle = MessagesUtil.getMessage("response.dns_not_found.title");

                assertThat("Expected \"response.dns_not_found.title\" message in body of error response", responseBody, containsString(hostNotFoundTitle));
                assertThat("Expected URL in body of error response", responseBody, containsString(url));
            }
        }
    }

    @Test
    public void testConnectionRefused() throws IOException {
        String url = "http://127.0.3.4:62663";

        try (CloseableHttpResponse response = getResponseFromHost(url)) {
            assertEquals("Expected 502 error due to connection failure", 502, response.getStatusLine().getStatusCode());

            //TODO: determine if it is possible, or desirable, to modify the error response body when using LittleProxy
            if (!Boolean.getBoolean("bmp.use.littleproxy")) {
                String responseBody = IOUtils.toStringAndClose(response.getEntity().getContent());

                String connectionFailureTitle = MessagesUtil.getMessage("response.conn_failure.title");

                assertThat("Expected \"response.conn_failure.title\" message in body of error response", responseBody, containsString(connectionFailureTitle));
                assertThat("Expected URL in body of error response", responseBody, containsString(url));
            }
        }
    }

    @Test
    public void testConnectionTimeout() throws IOException {
        // this test fails for littleproxy implementation because the connection timeout cannot be changed after the proxy is created
        //TODO: see if there is a way to change the littleproxy connection timeout after it is initialized
        assumeFalse(Boolean.getBoolean("bmp.use.littleproxy"));

        proxy.setConnectionTimeout(1);

        String url = "http://1.2.3.4:62663";

        try (CloseableHttpResponse response = getResponseFromHost(url)) {
            assertEquals("Expected 504 error due to connection timeout", 504, response.getStatusLine().getStatusCode());

            //TODO: determine if it is possible, or desirable, to modify the error response body when using LittleProxy
            if (!Boolean.getBoolean("bmp.use.littleproxy")) {
                String responseBody = IOUtils.toStringAndClose(response.getEntity().getContent());

                String networkTimeoutTitle = MessagesUtil.getMessage("response.net_timeout.title");

                assertThat("Expected \"response.net_timeout.title\" message in body of error response", responseBody, containsString(networkTimeoutTitle));
                assertThat("Expected URL in body of error response", responseBody, containsString(url));
            }
        }
    }

    //TODO: add additional tests for other error conditions
}
