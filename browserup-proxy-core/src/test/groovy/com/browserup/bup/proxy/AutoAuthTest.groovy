package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.auth.AuthType
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.github.tomakehurst.wiremock.client.WireMock
import io.netty.handler.codec.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.NottableString

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.notFound
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static org.junit.Assert.assertEquals
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

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
        def url = '/basicAuthHttp'
        stubFor(
                get(urlEqualTo(url)).
                        withHeader('Authorization', equalTo('Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==')).
                        willReturn(ok().
                                withBody('success')))

        proxy = new BrowserUpProxyServer()
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/basicAuthHttp")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)

            verify(1, getRequestedFor(urlEqualTo(url)))
        }
    }

    @Test
    void testBasicAuthAddedToHttpsRequest() {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        def url = '/basicAuthHttp'
        stubFor(
                get(urlEqualTo(url)).
                        withHeader('Authorization', equalTo('Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==')).
                        willReturn(ok().
                                withBody('success')))

        proxy = new BrowserUpProxyServer()
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/basicAuthHttp")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)

            verify(1, getRequestedFor(urlEqualTo(url)))
        }
    }

    @Test
    void testCanStopBasicAuth() {
        def url = '/basicAuthHttp'
        stubFor(
                get(urlEqualTo(url)).
                        withHeader('Authorization', WireMock.absent()).
                        willReturn(ok().
                                withBody('success')))

        proxy = new BrowserUpProxyServer()
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        proxy.stopAutoAuthorization("localhost")

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/basicAuthHttp")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)

            verify(1, getRequestedFor(urlEqualTo(url)))
        }
    }
}
