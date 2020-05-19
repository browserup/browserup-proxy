/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy


import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.impl.DefaultHttpProxyServer

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.assertEquals

class ChainedProxyAuthTest extends MockServerTest {
    MitmProxyServer proxy

    HttpProxyServer upstreamProxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }

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

//    @Test
//    void testAutoProxyAuthFailure() {
//        String proxyUser = "proxyuser"
//        String proxyPassword = "proxypassword"
//
//        upstreamProxy = DefaultHttpProxyServer.bootstrap()
//                .withProxyAuthenticator(new ProxyAuthenticator() {
//            @Override
//            boolean authenticate(String user, String password) {
//                return proxyUser.equals(user) && proxyPassword.equals(password)
//            }
//
//            @Override
//            String getRealm() {
//                return "some-realm"
//            }
//        })
//                .withPort(0)
//                .start()
//
//        def stubUrl = "/proxyauth"
//        stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withStatus(500).withBody("shouldn't happen")))
//
//        proxy = new BrowserUpProxyServer()
//        proxy.setChainedProxy(upstreamProxy.getListenAddress())
//        proxy.chainedProxyAuthorization(proxyUser, "wrongpassword", AuthType.BASIC)
//        proxy.setTrustAllServers(true)
//        proxy.start()
//
//        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
//            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/proxyauth"))
//            assertEquals("Expected to receive a Bad Gateway due to incorrect proxy authentication credentials", 502, response.getStatusLine().statusCode)
//        }
//
//        verify(lessThan(1), getRequestedFor(urlEqualTo(stubUrl)))
//    }
}
