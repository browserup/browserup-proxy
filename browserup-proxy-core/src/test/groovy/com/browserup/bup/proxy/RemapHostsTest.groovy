/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static org.junit.Assert.assertEquals

/**
 * Tests host remapping using the {@link com.browserup.bup.proxy.dns.AdvancedHostResolver#remapHost(java.lang.String, java.lang.String)}
 * and related methods exposes by {@link BrowserUpProxy#getHostNameResolver()}.
 */
class RemapHostsTest extends MockServerTest {
    private BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testRemapHttpHost() {
        // mock up a response to serve

        def stubUrl = "/remapHttpHost"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)

        proxy.getHostNameResolver().remapHost("www.someaddress.notreal", "localhost")

        proxy.start()

        int proxyPort = proxy.getPort()

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://www.someaddress.notreal:${mockServerPort}/remapHttpHost")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testRemapHttpsHost() {
        // mock up a response to serve
        def stubUrl = "/remapHttpsHost"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)

        proxy.getHostNameResolver().remapHost("www.someaddress.notreal", "localhost")

        proxy.start()

        int proxyPort = proxy.getPort()

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://www.someaddress.notreal:${mockServerHttpsPort}/remapHttpsHost")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }
}
