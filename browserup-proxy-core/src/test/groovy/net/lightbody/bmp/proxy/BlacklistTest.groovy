package net.lightbody.bmp.proxy

import net.lightbody.bmp.BrowserMobProxy
import net.lightbody.bmp.BrowserMobProxyServer
import net.lightbody.bmp.proxy.test.util.MockServerTest
import net.lightbody.bmp.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.mockserver.matchers.Times

import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class BlacklistTest extends MockServerTest {
    BrowserMobProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testBlacklistedHttpRequestReturnsBlacklistStatusCode() {
        proxy = new BrowserMobProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blacklistRequests("http://www\\.blacklisted\\.domain/.*", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("http://www.blacklisted.domain/someresource"))
            assertEquals("Did not receive blacklisted status code in response", 405, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertThat("Expected blacklisted response to contain 0-length body", responseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testBlacklistedHttpsRequestReturnsBlacklistStatusCode() {
        // need to set up a mock server to handle the CONNECT, since that is not blacklisted
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/thisrequestshouldnotoccur"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(500)
                .withBody("this URL should never be called"))

        proxy = new BrowserMobProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blacklistRequests("https://localhost:${mockServerPort}/.*", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerPort}/thisrequestshouldnotoccur"))
            assertEquals("Did not receive blacklisted status code in response", 405, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertThat("Expected blacklisted response to contain 0-length body", responseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testCanBlacklistSingleHttpResource() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/blacklistedresource"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(500)
                .withBody("this URL should never be called"))

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/nonblacklistedresource"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(200)
                .withBody("not blacklisted"))

        proxy = new BrowserMobProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blacklistRequests("http://localhost:${mockServerPort}/blacklistedresource", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse nonBlacklistedResourceResponse = it.execute(new HttpGet("http://localhost:${mockServerPort}/nonblacklistedresource"))
            assertEquals("Did not receive blacklisted status code in response", 200, nonBlacklistedResourceResponse.getStatusLine().getStatusCode())

            String nonBlacklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonBlacklistedResourceResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "not blacklisted", nonBlacklistedResponseBody)

            CloseableHttpResponse blacklistedResourceResponse = it.execute(new HttpGet("http://localhost:${mockServerPort}/blacklistedresource"))
            assertEquals("Did not receive blacklisted status code in response", 405, blacklistedResourceResponse.getStatusLine().getStatusCode())

            String blacklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blacklistedResourceResponse.getEntity().getContent())
            assertThat("Expected blacklisted response to contain 0-length body", blacklistedResponseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testCanBlacklistSingleHttpsResource() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/blacklistedresource"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(500)
                .withBody("this URL should never be called"))

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/nonblacklistedresource"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(200)
                .withBody("not blacklisted"))

        proxy = new BrowserMobProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blacklistRequests("https://localhost:${mockServerPort}/blacklistedresource", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse nonBlacklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerPort}/nonblacklistedresource"))
            assertEquals("Did not receive blacklisted status code in response", 200, nonBlacklistedResourceResponse.getStatusLine().getStatusCode())

            String nonBlacklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonBlacklistedResourceResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "not blacklisted", nonBlacklistedResponseBody)

            CloseableHttpResponse blacklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerPort}/blacklistedresource"))
            assertEquals("Did not receive blacklisted status code in response", 405, blacklistedResourceResponse.getStatusLine().getStatusCode())

            String blacklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blacklistedResourceResponse.getEntity().getContent())
            assertThat("Expected blacklisted response to contain 0-length body", blacklistedResponseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testCanBlacklistConnectExplicitly() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/blacklistconnect"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(500)
                .withBody("this URL should never be called"))

        proxy = new BrowserMobProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        // CONNECT requests don't contain the path to the resource, only the server and port
        proxy.blacklistRequests("https://localhost:${mockServerPort}", 405, "CONNECT")

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse blacklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerPort}/blacklistconnect"))
            assertEquals("Did not receive blacklisted status code in response", 405, blacklistedResourceResponse.getStatusLine().getStatusCode())

            String blacklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blacklistedResourceResponse.getEntity().getContent())
            assertThat("Expected blacklisted response to contain 0-length body", blacklistedResponseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testBlacklistDoesNotApplyToCONNECT() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/connectNotBlacklisted"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(200)
                .withBody("success"))

        proxy = new BrowserMobProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        // HTTP CONNECTs should not be blacklisted unless the method is explicitly specified
        proxy.blacklistRequests("https://localhost:${mockServerPort}", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerPort}/connectNotBlacklisted"))
            assertEquals("Expected to receive response from mock server after successful CONNECT", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Expected to receive HTTP 200 and success message from server", "success", responseBody)
        }
    }
}
