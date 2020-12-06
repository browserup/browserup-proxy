package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.littleshoot.proxy.*
import org.littleshoot.proxy.impl.DefaultHttpProxyServer
import org.littleshoot.proxy.impl.ProxyUtils

import java.nio.charset.Charset

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.assertEquals

@org.junit.Ignore
class NonProxyChainTest extends MockServerTest {

    private BrowserUpProxy proxy

    HttpProxyServer upstreamProxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }

        upstreamProxy?.abort()
    }

    /**
     * This testcase will set up a upstream proxy that is blocking all requests containing "localhost:"
     * Then it will setup a proxy with that upstream proxy
     * Then it will call an address containing "localhost:mockport/"
     * This will end up in a 502, because the request is processed to the upstream proxy, which will deny the request.
     */
    @Test
    void testUpStreamProxyWithoutNonProxy() {

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withFiltersSource(getFiltersSource())
                .withPort(0)
                .start()

        def stub = "/"
        stubFor(get(urlEqualTo(stub)).willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {

            CloseableHttpResponse response = it.execute(new HttpGet("http://localhost:${mockServerPort}/"))
            assertEquals("Did not receive HTTP 502 from mock server", 502, response.getStatusLine().getStatusCode())
        }

        verify(0, getRequestedFor(urlEqualTo("/")))
    }

    /**
     * This testcase will set up a upstream proxy that is blocking all requests containing "localhost:"
     * Then it will setup a proxy with that upstream proxy and configure a nonProxyHost "localhost"
     * Then it will call an address containing "localhost:mockport"
     * This will end up in a 200, because the request is NOT processed to the upstream proxy due the nonProxySetting
     */
    @Test
    void testUpStreamProxyWithNonProxy() {

        List<String> objects = new ArrayList<>()
        objects.add("localhost")

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withFiltersSource(getFiltersSource())
                .withPort(0)
                .start()

        def stub = "/"
        stubFor(get(urlEqualTo(stub)).willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.setChainedProxyNonProxyHosts(objects)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {

            CloseableHttpResponse response = it.execute(new HttpGet("http://localhost:${mockServerPort}/"))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlEqualTo("/")))
    }

    /**
     * This testcase will set up a upstream proxy that is blocking all requests containing "localhost:"
     * Then it will setup a proxy with that upstream proxy and configure a nonProxyHost "*"
     * Then it will call an address containing "localhost:mockport"
     * This will end up in a 200, because the request is NOT processed to the upstream proxy due the nonProxySetting
     */
    @Test
    void testUpStreamProxyWithNonProxyWildcard() {

        List<String> objects = new ArrayList<>()
        objects.add("*")

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withFiltersSource(getFiltersSource())
                .withPort(0)
                .start()

        def stub = "/"
        stubFor(get(urlEqualTo(stub)).willReturn(ok().withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.setChainedProxy(upstreamProxy.getListenAddress())
        proxy.setChainedProxyNonProxyHosts(objects)
        proxy.setTrustAllServers(true)
        proxy.start()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {

            CloseableHttpResponse response = it.execute(new HttpGet("http://localhost:${mockServerPort}/"))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlEqualTo("/")))
    }

    /**
     * Provides a HttpFiltersSource for configuring an upstream proxy while blocklisting "external.domain.com" and returning 502 Status Code.
     * @return
     */
    private HttpFiltersSource getFiltersSource() {

        return new HttpFiltersSourceAdapter() {

            @Override
            HttpFilters filterRequest(HttpRequest originalRequest) {

                return new HttpFiltersAdapter(originalRequest) {

                    @Override
                    HttpResponse clientToProxyRequest(HttpObject httpObject) {

                        if (httpObject instanceof HttpRequest) {
                            HttpRequest request = (HttpRequest) httpObject

                            System.out.println("Method URI : " + request.method() + " " + request.uri())

                            if (request.uri().contains("localhost:")) {
                                return getBadGatewayResponse()
                            }
                        }
                        return null
                    }

                    private HttpResponse getBadGatewayResponse() {
                        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                                .concat("<html><head>\n")
                                .concat("<title>Bad Gateway</title>\n")
                                .concat("</head><body>\n")
                                .concat("An error occurred")
                                .concat("</body></html>\n")
                        byte[] bytes = body.getBytes(Charset.forName("UTF-8"))
                        ByteBuf content = Unpooled.copiedBuffer(bytes)
                        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content)
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length)
                        response.headers().set("Content-Type", "text/html; charset=UTF-8")
                        response.headers().set("Date", ProxyUtils.formatDate(new Date()))
                        response.headers().set(HttpHeaderNames.CONNECTION, "close")
                        return response
                    }
                }
            }
        }
    }
}

