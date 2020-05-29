/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.auth.AuthType
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.ProxyAuthenticator
import org.littleshoot.proxy.extras.SelfSignedSslEngineSource
import org.littleshoot.proxy.impl.DefaultHttpProxyServer

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ChainedProxyAuthTest extends MockServerTest {
    MitmProxyServer proxy

    MitmProxyServer upstreamMitmProxy

    HttpProxyServer upstreamProxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
        upstreamMitmProxy?.abort()
        upstreamProxy?.abort()
    }

    @Test
    void testUpstreamProxyIsDown() {
        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withPort(0)
                .start()

        def stubUrl = "/proxyauth"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.setTrustAllServers(true)
        proxy.start()
        upstreamProxy.stop()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            def result = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/proxyauth"))
            assertEquals("Did not receive 502 BAD GATEWAY from mitmproxy", 502, result.original.code)
        }

        verify(0, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testMitmproxyUsesUpstreamProxy() {
        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withPort(0)
                .start()

        def stubUrl = "/proxyauth"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/proxyauth")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }


    @Test
    @Ignore
    void testUpstreamAndDownstreamProxiesGetRequestIfNonProxyHostDoNotMatch() {
        upstreamMitmProxy = new MitmProxyServer()
        upstreamMitmProxy.setTrustAllServers(true)
        upstreamMitmProxy.start()

        def stubUrl = "/proxyauth"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()

        proxy.setChainedProxy(new InetSocketAddress("localhost", upstreamMitmProxy.getPort()))
        proxy.setChainedProxyHTTPS(true)
        proxy.setTrustAllServers(true)
        proxy.setChainedProxyNonProxyHosts(['bbc.com'])
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/proxyauth")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        def downStreamHar = proxy.getHar()
        def upStreamHar = upstreamMitmProxy.getHar()

        assertEquals("Expected to get exactly one entry in har from downstream proxy", 1, downStreamHar.log.entries.size())
        assertEquals("Expected to get exactly one entry in har from upstream proxy", 1, upStreamHar.log.entries.size())

        assertEquals("Expected to get the same request URL in entries from downstream and upstream proxies",
                downStreamHar.log.entries.first().request.url,
                upStreamHar.log.entries.first().request.url)

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    @Ignore
    void testUpstreamProxyDoesNotGetRequestIfNonProxyHostMatch() {
        def stubUrl = "/proxyauth"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        upstreamMitmProxy = new MitmProxyServer()
        upstreamMitmProxy.setTrustAllServers(true)
        upstreamMitmProxy.start()

        proxy = new MitmProxyServer()

        proxy.setChainedProxy(new InetSocketAddress("localhost", upstreamMitmProxy.getPort()))
        proxy.setChainedProxyHTTPS(true)
        proxy.setTrustAllServers(true)
        proxy.setChainedProxyNonProxyHosts(['*localhost*'])
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/proxyauth")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        def downStreamHar = proxy.getHar()
        def upStreamHar = upstreamMitmProxy.getHar()

        assertEquals("Expected to get exactly one entry in har from downstream proxy", 1, downStreamHar.log.entries.size())
        assertEquals("Expected to get exactly no entries in har from upstream proxy", 0, upStreamHar.log.entries.size())

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testMitmproxyUsesHttpsUpstreamProxy() {
        upstreamMitmProxy = new MitmProxyServer()
        upstreamMitmProxy.setTrustAllServers(true)
        upstreamMitmProxy.start(Collections.emptyList())

        def stubUrl = "/proxyauth"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()

        proxy.setChainedProxy(new InetSocketAddress("localhost", upstreamMitmProxy.getPort()))
        proxy.setChainedProxyHTTPS(true)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/proxyauth")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testAutoProxyAuthSuccessful() {
        String proxyUser = "proxyuser"
        String proxyPassword = "proxypassword"

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withProxyAuthenticator(new ProxyAuthenticator() {
            @Override
            boolean authenticate(String user, String password) {
                return proxyUser.equals(user) && proxyPassword.equals(password)
            }

            @Override
            String getRealm() {
                return "some-realm"
            }
        })
                .withPort(0)
                .start()

        def stubUrl = "/proxyauth"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.setTrustAllServers(true)
        proxy.chainedProxyAuthorization(proxyUser, proxyPassword, AuthType.BASIC)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/proxyauth")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testAutoProxyAuthFailure() {
        String proxyUser = "proxyuser"
        String proxyPassword = "proxypassword"

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withProxyAuthenticator(new ProxyAuthenticator() {
            @Override
            boolean authenticate(String user, String password) {
                return proxyUser.equals(user) && proxyPassword.equals(password)
            }

            @Override
            String getRealm() {
                return "some-realm"
            }
        })
                .withPort(0)
                .start()

        def stubUrl = "/proxyauth"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withStatus(500).withBody("shouldn't happen")))

        proxy = new MitmProxyServer()
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.chainedProxyAuthorization(proxyUser, "wrongpassword", AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/proxyauth"))
            assertEquals("Expected to receive a Bad Gateway due to incorrect proxy authentication credentials", 502, response.getStatusLine().statusCode)
        }

        verify(lessThan(1), getRequestedFor(urlEqualTo(stubUrl)))
    }
}
