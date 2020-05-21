/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.auth.AuthType
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.github.tomakehurst.wiremock.matching.AbsentPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
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
import static com.github.tomakehurst.wiremock.http.HttpHeader.absent
import static org.junit.Assert.assertEquals

class AutoAuthTest extends MockServerTest {
    BrowserUpProxy proxy

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

        proxy = new BrowserUpProxyServer()
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

        proxy = new BrowserUpProxyServer()
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
    void testBasicAuthWithWildcardAddedToHttpsRequest() {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        def stubUrl = "/basicAuthHttp"

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Authorization", new EqualToPattern("Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA=="))
                .willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        // * (wildcard) should match "localhost"
        proxy.autoAuthorization("*", "testUsername", "testPassword", AuthType.BASIC)
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

        proxy = new BrowserUpProxyServer()
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
