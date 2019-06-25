package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
class BlacklistTest extends MockServerTest {
    BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testBlacklistedHttpRequestReturnsBlacklistStatusCode() {
        proxy = new BrowserUpProxyServer()
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
        def stubUrl = "/thisrequestshouldnotoccur"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blacklistRequests("https://localhost:${mockServerHttpsPort}/.*", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/thisrequestshouldnotoccur"))
            assertEquals("Did not receive blacklisted status code in response", 405, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertThat("Expected blacklisted response to contain 0-length body", responseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testCanBlacklistSingleHttpResource() {
        def stubUrl1 = "/blacklistedresource"
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")))

        def stubUrl2 = "/nonblacklistedresource"
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blacklisted")))

        proxy = new BrowserUpProxyServer()
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
        def stubUrl1 = "/blacklistedresource"
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")))

        def stubUrl2 = "/nonblacklistedresource"
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blacklisted")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blacklistRequests("https://localhost:${mockServerHttpsPort}/blacklistedresource", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse nonBlacklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/nonblacklistedresource"))
            assertEquals("Did not receive blacklisted status code in response", 200, nonBlacklistedResourceResponse.getStatusLine().getStatusCode())

            String nonBlacklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonBlacklistedResourceResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "not blacklisted", nonBlacklistedResponseBody)

            CloseableHttpResponse blacklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/blacklistedresource"))
            assertEquals("Did not receive blacklisted status code in response", 405, blacklistedResourceResponse.getStatusLine().getStatusCode())

            String blacklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blacklistedResourceResponse.getEntity().getContent())
            assertThat("Expected blacklisted response to contain 0-length body", blacklistedResponseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testCanBlacklistConnectExplicitly() {
        def stubUrl1 = "/blacklistconnect"
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")))

        proxy = new BrowserUpProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        // CONNECT requests don't contain the path to the resource, only the server and port
        proxy.blacklistRequests("https://localhost:${mockServerHttpsPort}", 405, "CONNECT")

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse blacklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/blacklistconnect"))
            assertEquals("Did not receive blacklisted status code in response", 405, blacklistedResourceResponse.getStatusLine().getStatusCode())

            String blacklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blacklistedResourceResponse.getEntity().getContent())
            assertThat("Expected blacklisted response to contain 0-length body", blacklistedResponseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testBlacklistDoesNotApplyToCONNECT() {
        def stubUrl = "/connectNotBlacklisted"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        // HTTP CONNECTs should not be blacklisted unless the method is explicitly specified
        proxy.blacklistRequests("https://localhost:${mockServerHttpsPort}", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/connectNotBlacklisted"))
            assertEquals("Expected to receive response from mock server after successful CONNECT", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Expected to receive HTTP 200 and success message from server", "success", responseBody)
        }
    }
}
