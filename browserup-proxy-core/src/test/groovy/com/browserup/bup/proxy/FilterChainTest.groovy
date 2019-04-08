package com.browserup.bup.proxy

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.littleshoot.proxy.HttpFilters
import org.littleshoot.proxy.HttpFiltersAdapter
import org.littleshoot.proxy.HttpFiltersSourceAdapter

import java.util.concurrent.atomic.AtomicBoolean

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Tests for the {@link com.browserup.bup.filters.BrowserUpHttpFilterChain}.
 */
class FilterChainTest extends MockServerTest {
    private BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testFilterExceptionDoesNotAbortRequest() {
        // tests that an exception in one filter does not prevent the request from completing
        def url = '/testfilterexceptionpreservesrequest'
        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody('success')))

        proxy = new BrowserUpProxyServer()

        proxy.addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new ThrowExceptionFilter()
            }
        })

        proxy.start()

        String requestUrl = "http://localhost:${mockServerPort}/testfilterexceptionpreservesrequest"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)

            verify(1, getRequestedFor(urlEqualTo(url)))
        }
    }

    @Test
    void testFilterExceptionDoesNotAbortFilterChain() {
        // tests that an exception in the first filter in a filter chain does not prevent subsequent filters from being invoked
        def url = '/testfilterexceptionpreserveschain'
        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody('success')))

        proxy = new BrowserUpProxyServer()

        proxy.addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new ThrowExceptionFilter()
            }
        })

        // rather than test every filter method (which would be verbose), test three filter methods that are representative
        // of the entire request-response lifecycle are still fired
        final AtomicBoolean clientToProxyRequest = new AtomicBoolean()
        final AtomicBoolean serverToProxyResponse = new AtomicBoolean()
        final AtomicBoolean proxyToClientResponse = new AtomicBoolean()

        proxy.addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    HttpResponse clientToProxyRequest(HttpObject httpObject) {
                        clientToProxyRequest.set(true)
                        return super.clientToProxyRequest(httpObject)
                    }

                    @Override
                    HttpObject serverToProxyResponse(HttpObject httpObject) {
                        serverToProxyResponse.set(true)
                        return super.serverToProxyResponse(httpObject)
                    }

                    @Override
                    HttpObject proxyToClientResponse(HttpObject httpObject) {
                        proxyToClientResponse.set(true)
                        return super.proxyToClientResponse(httpObject)
                    }
                }
            }
        })

        proxy.start()

        String requestUrl = "http://localhost:${mockServerPort}/testfilterexceptionpreserveschain"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        assertTrue("Expected second filter method to be invoked after first filter threw exception", clientToProxyRequest.get())
        assertTrue("Expected second filter method to be invoked after first filter threw exception", serverToProxyResponse.get())
        assertTrue("Expected second filter method to be invoked after first filter threw exception", proxyToClientResponse.get())

        verify(1, getRequestedFor(urlEqualTo(url)))
    }

    @Test
    void testRequestResponseFilterExceptionsDoNotAbortRequest() {
        // tests that exceptions thrown by the RequestFilter and ResponseFilter do not abort the request
        def url = '/testrequestresponsefilterpreservesrequest'
        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody('success')))

        proxy = new BrowserUpProxyServer()

        proxy.addRequestFilter({ a, b, c ->
            throw new RuntimeException("Throwing exception from RequestFilter")
        })

        proxy.addResponseFilter({ a, b, c ->
            throw new RuntimeException("Throwing exception from ResponseFilter")
        })

        proxy.start()

        String requestUrl = "http://localhost:${mockServerPort}/testrequestresponsefilterpreservesrequest"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        verify(1, getRequestedFor(urlEqualTo(url)))
    }

    @Test
    void testRequestResponseFilterExceptionsDoNotAbortFilterChain() {
        // tests that exceptions thrown by the RequestFilter and ResponseFilter do not prevent subsequent filters from being invoked
        def url = '/testrequestresponsefilterpreserveschain'
        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody('success')))

        proxy = new BrowserUpProxyServer()

        final AtomicBoolean secondRequestFilterInvoked = new AtomicBoolean()
        final AtomicBoolean secondResponseFilterInvoked = new AtomicBoolean()

        proxy.addRequestFilter({ a, b, c ->
            // actually the second filter invoked, since the following addRequestFilter will place itself at the front of the filter chain
            secondRequestFilterInvoked.set(true)
        })

        proxy.addRequestFilter({ a, b, c ->
            assertFalse("Did not expect second request filter to be invoked yet", secondRequestFilterInvoked.get())
            throw new RuntimeException("Throwing exception from RequestFilter")
        })

        proxy.addResponseFilter({ a, b, c ->
            assertFalse("Did not expect second response filter to be invoked yet", secondResponseFilterInvoked.get())
            throw new RuntimeException("Throwing exception from ResponseFilter")
        })

        proxy.addResponseFilter({ a, b, c ->
            secondResponseFilterInvoked.set(true)
        })

        proxy.start()

        String requestUrl = "http://localhost:${mockServerPort}/testrequestresponsefilterpreserveschain"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        assertTrue("Expected second request filter to be invoked", secondRequestFilterInvoked.get())
        assertTrue("Expected second response filter to be invoked", secondResponseFilterInvoked.get())

        verify(1, getRequestedFor(urlEqualTo(url)))
    }

    /**
     * An HttpFilters implementation that throws an exception from every filter method.
     */
    static class ThrowExceptionFilter implements HttpFilters {

        @Override
        HttpResponse clientToProxyRequest(HttpObject httpObject) {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        HttpResponse proxyToServerRequest(HttpObject httpObject) {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerRequestSending() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerRequestSent() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        HttpObject serverToProxyResponse(HttpObject httpObject) {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void serverToProxyResponseTimedOut() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void serverToProxyResponseReceiving() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void serverToProxyResponseReceived() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        HttpObject proxyToClientResponse(HttpObject httpObject) {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerConnectionQueued() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        InetSocketAddress proxyToServerResolutionStarted(String resolvingServerHostAndPort) {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerResolutionFailed(String hostAndPort) {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerResolutionSucceeded(String serverHostAndPort, InetSocketAddress resolvedRemoteAddress) {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerConnectionStarted() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerConnectionSSLHandshakeStarted() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerConnectionFailed() {
            throw new RuntimeException("Throwing exception from filter")
        }

        @Override
        void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
            throw new RuntimeException("Throwing exception from filter")
        }
    }
}
