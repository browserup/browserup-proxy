/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.github.tomakehurst.wiremock.client.WireMock
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static org.junit.Assert.assertEquals

class RewriteRuleTest extends MockServerTest {
    private BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testRewriteHttpUrl() {
        def stubUrl = "/.*"
        stubFor(get(urlMatching(stubUrl))
                .withQueryParam("originalDomain", WireMock.equalTo("yahoo"))
                .withQueryParam("param1", WireMock.equalTo("value1"))
                .willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.rewriteUrl('http://www\\.(yahoo|bing)\\.com/\\?(\\w+)=(\\w+)', 'http://localhost:' + mockServerPort + '/?originalDomain=$1&$2=$3')
        proxy.setTrustAllServers(true)
        proxy.start()

        String requestUrl = "http://www.yahoo.com?param1=value1"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testRewriteHttpsUrl() {
        // HTTPS URLs cannot currently be rewritten to another domain, so verify query parameters can be rewritten

        def stubUrl = "/.*"
        stubFor(get(urlMatching(stubUrl))
                .withQueryParam("firstParam", WireMock.equalTo("param1"))
                .withQueryParam("firstValue", WireMock.equalTo("value1"))
                .willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.rewriteUrl('https://localhost:' + mockServerHttpsPort + '/\\?(\\w+)=(\\w+)', 'https://localhost:' + mockServerHttpsPort + '/?firstParam=$1&firstValue=$2')
        proxy.setTrustAllServers(true)
        proxy.start()

        String requestUrl = "https://localhost:$mockServerHttpsPort?param1=value1"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }
}
