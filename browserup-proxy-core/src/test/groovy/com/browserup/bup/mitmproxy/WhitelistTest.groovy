/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.MitmProxyServer
import com.browserup.bup.filters.AllowlistFilter
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpVersion
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.junit.Assert.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class AllowlistTest extends MockServerTest {
    MitmProxyServer proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testAllowlistCannotShortCircuitCONNECT() {
        HttpRequest request = mock(HttpRequest.class)
        when(request.method()).thenReturn(HttpMethod.CONNECT)
        when(request.uri()).thenReturn('somedomain.com:443')
        when(request.protocolVersion()).thenReturn(HttpVersion.HTTP_1_1)
        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext)

        // create a allowlist filter that allowlists no requests (i.e., all requests should return the specified HTTP 500 status code)
        AllowlistFilter filter = new AllowlistFilter(request, mockCtx, true, 500, [])
        HttpResponse response = filter.clientToProxyRequest(request)

        assertNull("Allowlist short-circuited HTTP CONNECT. Expected all HTTP CONNECTs to be allowlisted.", response)
    }

    @Test
    void testNonAllowlistedHttpRequestReturnsAllowlistStatusCode() {
        proxy = new MitmProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.allowlistRequests(["http://localhost/.*"], 500)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("http://www.someother.domain/someresource"))
            assertEquals("Did not receive allowlist status code in response", 500, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertThat("Expected allowlist response to contain 0-length body", responseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testNonAllowlistedHttpsRequestReturnsAllowlistStatusCode() {
        String url = "/nonallowlistedresource"

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("should never be returned")))

        proxy = new MitmProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.allowlistRequests(["https://some-other-domain/.*"], 500)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/nonallowlistedresource"))
            assertEquals("Did not receive allowlist status code in response", 500, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertThat("Expected allowlist response to contain 0-length body", responseBody, isEmptyOrNullString())
        }
    }

    @Test
    void testAllowlistedHttpRequestNotShortCircuited() {
        String url = "/allowlistedresource"

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("allowlisted")))

        proxy = new MitmProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.allowlistRequests(["http://localhost:${mockServerPort}/.*".toString()], 500)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("http://localhost:${mockServerPort}/allowlistedresource"))
            assertEquals("Did not receive expected response from mock server for allowlisted url", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response body from mock server for allowlisted url", "allowlisted", responseBody)
        }
    }

    @Test
    void testAllowlistedHttpsRequestNotShortCircuited() {
        String url = "/allowlistedresource"

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("allowlisted")))

        proxy = new MitmProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.allowlistRequests(["https://localhost:${mockServerHttpsPort}/.*".toString()], 500)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/allowlistedresource"))
            assertEquals("Did not receive expected response from mock server for allowlisted url", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response body from mock server for allowlisted url", "allowlisted", responseBody)
        }
    }

    @Test
    void testCanAllowlistSpecificHttpResource() {
        String url = "/allowlistedresource"

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("allowlisted")))

        String url2 = "/nonallowlistedresource"

        stubFor(get(urlEqualTo(url2)).willReturn(ok().withBody("should never be returned")))

        proxy = new MitmProxyServer()
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.allowlistRequests(["http://localhost:${mockServerPort}/allowlistedresource".toString()], 500)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse nonAllowlistedResponse = it.execute(new HttpGet("http://localhost:${mockServerPort}/nonallowlistedresource"))
            assertEquals("Did not receive allowlist status code in response", 500, nonAllowlistedResponse.getStatusLine().getStatusCode())

            String nonAllowlistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonAllowlistedResponse.getEntity().getContent())
            assertThat("Expected allowlist response to contain 0-length body", nonAllowlistedResponseBody, isEmptyOrNullString())

            CloseableHttpResponse allowlistedResponse = it.execute(new HttpGet("http://localhost:${mockServerPort}/allowlistedresource"))
            assertEquals("Did not receive expected response from mock server for allowlisted url", 200, allowlistedResponse.getStatusLine().getStatusCode())

            String allowlistedResponseBody = NewProxyServerTestUtil.toStringAndClose(allowlistedResponse.getEntity().getContent())
            assertEquals("Did not receive expected response body from mock server for allowlisted url", "allowlisted", allowlistedResponseBody)
        }
    }

    @Test
    void testCanAllowlistSpecificHttpsResource() {
        String url = "/allowlistedresource"

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("allowlisted")))

        String url2 = "/nonallowlistedresource"

        stubFor(get(urlEqualTo(url2)).willReturn(ok().withBody("should never be returned")))

        proxy = new MitmProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.allowlistRequests(["https://localhost:${mockServerHttpsPort}/allowlistedresource".toString()], 500)

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            CloseableHttpResponse nonAllowlistedResponse = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/nonallowlistedresource"))
            assertEquals("Did not receive allowlist status code in response", 500, nonAllowlistedResponse.getStatusLine().getStatusCode())

            String nonAllowlistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonAllowlistedResponse.getEntity().getContent())
            assertThat("Expected allowlist response to contain 0-length body", nonAllowlistedResponseBody, isEmptyOrNullString())

            CloseableHttpResponse allowlistedResponse = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/allowlistedresource"))
            assertEquals("Did not receive expected response from mock server for allowlisted url", 200, allowlistedResponse.getStatusLine().getStatusCode())

            String allowlistedResponseBody = NewProxyServerTestUtil.toStringAndClose(allowlistedResponse.getEntity().getContent())
            assertEquals("Did not receive expected response body from mock server for allowlisted url", "allowlisted", allowlistedResponseBody)
        }
    }
}
