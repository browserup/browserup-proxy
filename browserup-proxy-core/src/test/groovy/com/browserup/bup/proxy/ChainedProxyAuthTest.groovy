/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.auth.AuthType
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.ProxyAuthenticator
import org.littleshoot.proxy.impl.DefaultHttpProxyServer

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.lessThan
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static org.junit.Assert.assertEquals

class ChainedProxyAuthTest extends MockServerTest {
    BrowserUpProxy proxy

    HttpProxyServer upstreamProxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }

        upstreamProxy?.abort()
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

        proxy = new BrowserUpProxyServer()
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.chainedProxyAuthorization(proxyUser, proxyPassword, AuthType.BASIC)
        proxy.setTrustAllServers(true)
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

        proxy = new BrowserUpProxyServer()
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
