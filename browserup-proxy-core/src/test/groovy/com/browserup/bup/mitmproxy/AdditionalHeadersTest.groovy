/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.github.tomakehurst.wiremock.matching.AbsentPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.assertEquals

class AdditionalHeadersTest extends MockServerTest {

    MitmProxyServer proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testAdditionalHeaderIsAdded() {
        def stubUrl = "/dummyPath"
        def customHeaderName = "CustomHeaderName"
        def customHeaderValue = "CustomHeaderValue"

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader(customHeaderName, new EqualToPattern(customHeaderValue))
                .willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.addHeader(customHeaderName, customHeaderValue)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}${stubUrl}")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testAdditionalHeadersAreAdded() {
        def stubUrl = "/dummyPath"

        def headers = [
                "CustomHeaderName1": "CustomHeaderValue1",
                "CustomHeaderName2": "CustomHeaderValue2"
        ]

        def stub = get(urlEqualTo(stubUrl))
                .willReturn(ok().withBody("success"))

        headers.each {
            stub.withHeader(it.key, new EqualToPattern(it.value))
        }

        stubFor(stub)

        proxy = new MitmProxyServer()
        proxy.addHeaders(headers)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}${stubUrl}")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testAdditionalHeadersAreAddedExceptOne() {
        def stubUrl = "/dummyPath"

        def headers = [
                "CustomHeaderName1": "CustomHeaderValue1",
                "CustomHeaderName2": "CustomHeaderValue2",
                "CustomHeaderName3": "CustomHeaderValue3"
        ]

        def stub = get(urlEqualTo(stubUrl))
                .willReturn(ok().withBody("success"))

        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(notFound()))

        headers.each {
            stub.withHeader(it.key, new EqualToPattern(it.value))
        }

        stubFor(stub)

        proxy = new MitmProxyServer()
        proxy.addHeaders(headers.take(2))
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            def responseCode = it.execute(new HttpGet("http://localhost:${mockServerPort}${stubUrl}")).statusLine.statusCode
            assertEquals("Expected to get 404 response", 404, responseCode)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }


    @Test
    void testAdditionalHeadersAreAddedAndOneDeleted() {
        def stubUrl = "/dummyPath"

        def headers = [
                "CustomHeaderName1": "CustomHeaderValue1",
                "CustomHeaderName2": "CustomHeaderValue2",
                "CustomHeaderName3": "CustomHeaderValue3"
        ]

        def stub = get(urlEqualTo(stubUrl))
                .willReturn(ok().withBody("success"))

        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(notFound()))

        headers.each {
            stub.withHeader(it.key, new EqualToPattern(it.value))
        }

        stubFor(stub)

        proxy = new MitmProxyServer()
        proxy.addHeaders(headers)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}${stubUrl}")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        proxy.removeHeader(headers.entrySet().first().key)

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            def responseCode = it.execute(new HttpGet("http://localhost:${mockServerPort}${stubUrl}")).statusLine.statusCode
            assertEquals("Expected to get 404 response", 404, responseCode)
        }

        assertEquals("Expected to get 2 headers left after removing", 2, proxy.getAllHeaders().size())

        verify(2, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testAdditionalHeadersAreAddedAndAllDeleted() {
        def stubUrl = "/dummyPath"

        def headers = [
                "CustomHeaderName1": "CustomHeaderValue1",
                "CustomHeaderName2": "CustomHeaderValue2",
                "CustomHeaderName3": "CustomHeaderValue3"
        ]

        def stubWithoutHeaders = get(urlEqualTo(stubUrl))
                .willReturn(ok().withBody("success"))

        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(notFound()))

        headers.each {
            stubWithoutHeaders.withHeader(it.key, new AbsentPattern(it.value))
        }

        stubFor(stubWithoutHeaders)

        proxy = new MitmProxyServer()
        proxy.addHeaders(headers)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            def responseCode = it.execute(new HttpGet("http://localhost:${mockServerPort}${stubUrl}")).statusLine.statusCode
            assertEquals("Expected to get 404 response", 404, responseCode)
        }

        proxy.removeAllHeaders()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}${stubUrl}")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        assertEquals("Expected to get no headers left after removing", 0, proxy.getAllHeaders().size())

        verify(2, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testFailsIfAdditionalHeaderNotAdded() {
        def stubUrl = "/dummyPath"
        def customHeaderName = "CustomHeaderName"
        def customHeaderValue = "CustomHeaderValue"

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader(customHeaderName, new EqualToPattern(customHeaderValue))
                .willReturn(ok().withBody("success")))

        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(notFound()))

        proxy = new MitmProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            def responseCode = it.execute(new HttpGet("http://localhost:${mockServerPort}${stubUrl}")).statusLine.statusCode
            assertEquals("Expected to get 404 response", 404, responseCode)
        }
    }

}
