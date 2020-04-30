/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.assertEquals

class RewriteUrlFilterTest extends MockServerTest {
    MitmProxyServer proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testRewriteMultipleMatches() {
        def stubUrl = "/testRewriteHttpHost/finalModification"
        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Host",new EqualToPattern("localhost:${mockServerPort}"))
                .willReturn(ok()
                .withBody("success")))

        proxy = new MitmProxyServer()
        proxy.rewriteUrls([
                'http://www\\.someotherhost\\.com:(\\d+)/(\\w+)' : 'http://localhost:\\1/\\2',
                'http://localhost:(\\d+)/(\\w+)' : 'http://localhost:\\1/\\2/finalModification'
        ])

        proxy.start()

        String url = "http://www.someotherhost.com:${mockServerPort}/testRewriteHttpHost"
        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse firstResponse = it.execute(new HttpGet(url))
            assertEquals("Did not receive HTTP 200 from mock server", 200, firstResponse.getStatusLine().getStatusCode())

            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(firstResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody)

            CloseableHttpResponse secondResponse = it.execute(new HttpGet(url))
            assertEquals("Did not receive HTTP 200 from mock server", 200, secondResponse.getStatusLine().getStatusCode())

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(secondResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody)
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testRewriteHttpHost() {
        def stubUrl = "/testRewriteHttpHost"
        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Host",new EqualToPattern("localhost:${mockServerPort}"))
                .willReturn(ok()
                .withBody("success")))

        proxy = new MitmProxyServer()
        proxy.rewriteUrl('http://www\\.someotherhost\\.com:(\\d+)/(\\w+)', 'http://localhost:\\1/\\2')

        proxy.start()

        String url = "http://www.someotherhost.com:${mockServerPort}/testRewriteHttpHost"
        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse firstResponse = it.execute(new HttpGet(url))
            assertEquals("Did not receive HTTP 200 from mock server", 200, firstResponse.getStatusLine().getStatusCode())

            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(firstResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody)

            CloseableHttpResponse secondResponse = it.execute(new HttpGet(url))
            assertEquals("Did not receive HTTP 200 from mock server", 200, secondResponse.getStatusLine().getStatusCode())

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(secondResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody)
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testRewriteHttpResource() {
        def stubUrl = "/rewrittenresource"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success")))

        proxy = new MitmProxyServer()
        proxy.rewriteUrl('http://badhost:(\\d+)/badresource', 'http://localhost:\\1/rewrittenresource')

        proxy.start()

        String url = "http://badhost:${mockServerPort}/badresource"
        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(url)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody)

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(url)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody)
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testRewriteHttpsResource() {
        def stubUrl = "/rewrittenresource"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success")))

        proxy = new MitmProxyServer()
        proxy.setTrustAllServers(true)
        proxy.rewriteUrl('https://localhost:(\\d+)/badresource', 'https://localhost:\\1/rewrittenresource')

        proxy.start()

        String url = "https://localhost:${mockServerHttpsPort}/badresource"
        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(url)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody)

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(url)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody)
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)))
    }
}
