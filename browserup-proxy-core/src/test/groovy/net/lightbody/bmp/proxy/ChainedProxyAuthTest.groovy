package net.lightbody.bmp.proxy

import net.lightbody.bmp.BrowserMobProxy
import net.lightbody.bmp.BrowserMobProxyServer
import net.lightbody.bmp.proxy.auth.AuthType
import net.lightbody.bmp.proxy.test.util.MockServerTest
import net.lightbody.bmp.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.ProxyAuthenticator
import org.littleshoot.proxy.impl.DefaultHttpProxyServer
import org.mockserver.matchers.Times

import static org.junit.Assert.assertEquals
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class ChainedProxyAuthTest extends MockServerTest {
    BrowserMobProxy proxy

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

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/proxyauth"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withBody("success"))

        proxy = new BrowserMobProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.chainedProxyAuthorization(proxyUser, proxyPassword, AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerPort}/proxyauth")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };
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

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/proxyauth"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(500)
                .withBody("shouldn't happen"))

        proxy = new BrowserMobProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.chainedProxyAuthorization(proxyUser, "wrongpassword", AuthType.BASIC)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet("https://localhost:${mockServerPort}/proxyauth"))
            assertEquals("Expected to receive a Bad Gateway due to incorrect proxy authentication credentials", 502, response.getStatusLine().statusCode)
        };
    }
}
