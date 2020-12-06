/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

@org.junit.Ignore
class BlocklistTest extends MockServerTest {
    BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testBlocklistedHttpRequestReturnsBlocklistStatusCode() {
        proxy = new BrowserUpProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blocklistRequests("http://www\\.blocklisted\\.domain/.*", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("http://www.blocklisted.domain/someresource"))
            assertEquals("Did not receive blocklisted status code in response", 405, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testBlocklistedHttpRequestNotRecordedToHar() {
        proxy = new BrowserUpProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blocklistRequests("http://www\\.blocklisted\\.domain/.*", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("http://www.blocklisted.domain/someresource"))
            assertEquals("Did not receive blocklisted status code in response", 405, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, isEmptyOrNullString())
        }

        def har = proxy.getHar()

        assertFalse('Expected not to find blocklisted requests in har entries',
                har.log.entries.any { it.request.url.contains('blocklisted')} )
    }

    @Test
    void testBlocklistedHttpsRequestReturnsBlocklistStatusCode() {
        // need to set up a mock server to handle the CONNECT, since that is not blocklisted
        def stubUrl = "/thisrequestshouldnotoccur"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blocklistRequests("https://localhost:${mockServerHttpsPort}/.*", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/thisrequestshouldnotoccur"))
            assertEquals("Did not receive blocklisted status code in response", 405, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testCanBlocklistSingleHttpResource() {
        def stubUrl1 = "/blocklistedresource"
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")))

        def stubUrl2 = "/nonblocklistedresource"
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blocklisted")))

        proxy = new BrowserUpProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blocklistRequests("http://localhost:${mockServerPort}/blocklistedresource", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse nonBlocklistedResourceResponse = it.execute(new HttpGet("http://localhost:${mockServerPort}/nonblocklistedresource"))
            assertEquals("Did not receive blocklisted status code in response", 200, nonBlocklistedResourceResponse.getStatusLine().getStatusCode())

            String nonBlocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonBlocklistedResourceResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "not blocklisted", nonBlocklistedResponseBody)

            CloseableHttpResponse blocklistedResourceResponse = it.execute(new HttpGet("http://localhost:${mockServerPort}/blocklistedresource"))
            assertEquals("Did not receive blocklisted status code in response", 405, blocklistedResourceResponse.getStatusLine().getStatusCode())

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blocklistedResourceResponse.getEntity().getContent())
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testCanBlocklistSingleHttpsResource() {
        def stubUrl1 = "/blocklistedresource"
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")))

        def stubUrl2 = "/nonblocklistedresource"
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blocklisted")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.blocklistRequests("https://localhost:${mockServerHttpsPort}/blocklistedresource", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse nonBlocklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/nonblocklistedresource"))
            assertEquals("Did not receive blocklisted status code in response", 200, nonBlocklistedResourceResponse.getStatusLine().getStatusCode())

            String nonBlocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonBlocklistedResourceResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "not blocklisted", nonBlocklistedResponseBody)

            CloseableHttpResponse blocklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/blocklistedresource"))
            assertEquals("Did not receive blocklisted status code in response", 405, blocklistedResourceResponse.getStatusLine().getStatusCode())

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blocklistedResourceResponse.getEntity().getContent())
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testCanBlocklistConnectExplicitly() {
        def stubUrl1 = "/blocklistconnect"
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")))

        proxy = new BrowserUpProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        // CONNECT requests don't contain the path to the resource, only the server and port
        proxy.blocklistRequests("https://localhost:${mockServerHttpsPort}", 405, "CONNECT")

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse blocklistedResourceResponse = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/blocklistconnect"))
            assertEquals("Did not receive blocklisted status code in response", 405, blocklistedResourceResponse.getStatusLine().getStatusCode())

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blocklistedResourceResponse.getEntity().getContent())
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testBlocklistDoesNotApplyToCONNECT() {
        def stubUrl = "/connectNotBlocklisted"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        // HTTP CONNECTs should not be blocklisted unless the method is explicitly specified
        proxy.blocklistRequests("https://localhost:${mockServerHttpsPort}", 405)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/connectNotBlocklisted"))
            assertEquals("Expected to receive response from mock server after successful CONNECT", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Expected to receive HTTP 200 and success message from server", "success", responseBody)
        }
    }
}
