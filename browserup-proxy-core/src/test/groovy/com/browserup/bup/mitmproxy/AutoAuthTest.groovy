/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.auth.AuthType
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.assertEquals

class AutoAuthTest extends MockServerTest {
    MitmProxyServer proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testBasicAuthAddedToHttpRequest() {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        def stubUrl = "/basicAuthHttp"

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Authorization", new EqualToPattern("Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA=="))
                .willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/basicAuthHttp")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testBasicAuthAddedToHttpsRequest() {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        def stubUrl = "/basicAuthHttp"

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Authorization", new EqualToPattern("Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA=="))
                .willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/basicAuthHttp")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testCanStopBasicAuth() {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        def stubUrl = "/basicAuthHttp"

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Authorization", StringValuePattern.ABSENT)
                .willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        proxy.stopAutoAuthorization("localhost")

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/basicAuthHttp")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }
}
