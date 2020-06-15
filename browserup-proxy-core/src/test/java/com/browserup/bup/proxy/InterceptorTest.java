/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.google.common.io.ByteStreams;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.RequestFilter;
import com.browserup.bup.filters.RequestFilterAdapter;
import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.filters.ResponseFilterAdapter;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import com.browserup.bup.util.HttpMessageContents;
import com.browserup.bup.util.HttpMessageInfo;
import com.browserup.bup.util.HttpObjectUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@org.junit.Ignore
public class InterceptorTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testCanShortCircuitResponse() throws IOException {
        String url1 = "/regular200";
        stubFor(get(urlMatching(url1)).willReturn(ok().withBody("success")));

        String url2 = "/shortcircuit204";
        stubFor(get(urlMatching(url2)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicBoolean interceptorFired = new AtomicBoolean(false);
        final AtomicBoolean shortCircuitFired = new AtomicBoolean(false);

        proxy.addFirstHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                        if (httpObject instanceof HttpRequest) {
                            interceptorFired.set(true);

                            HttpRequest httpRequest = (HttpRequest) httpObject;

                            if (httpRequest.getMethod().equals(HttpMethod.GET) && httpRequest.uri().contains("/shortcircuit204")) {
                                HttpResponse httpResponse = new DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.NO_CONTENT);

                                shortCircuitFired.set(true);

                                return httpResponse;
                            }
                        }

                        return super.clientToProxyRequest(httpObject);
                    }
                };
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/regular200"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertTrue("Expected interceptor to fire", interceptorFired.get());
            assertFalse("Did not expected short circuit interceptor code to execute", shortCircuitFired.get());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);

            verify(1, getRequestedFor(urlEqualTo(url1)));
        }

        interceptorFired.set(false);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/shortcircuit204"));

            assertTrue("Expected interceptor to fire", interceptorFired.get());
            assertTrue("Expected interceptor to short-circuit response", shortCircuitFired.get());

            assertEquals("Expected interceptor to return a 204 (No Content)", 204, response.getStatusLine().getStatusCode());
            assertNull("Expected no entity attached to response", response.getEntity());
        }
    }

    @Test
    public void testCanModifyResponseBodyLarger() throws IOException {
        final String originalText = "The quick brown fox jumps over the lazy dog";
        final String newText = "The quick brown frog jumps over the lazy aardvark";

        testModifiedResponse(originalText, newText);
    }

    @Test
    public void testCanModifyResponseBodySmaller() throws IOException {
        final String originalText = "The quick brown fox jumps over the lazy dog";
        final String newText = "The quick brown fox jumped.";

        testModifiedResponse(originalText, newText);
    }

    @Test
    public void testCanModifyRequest() throws IOException {
        String url = "/modifyrequest";
        stubFor(
                get(urlEqualTo(url)).
                        willReturn(ok().
                                withBody("success").
                                withHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addFirstHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                        if (httpObject instanceof HttpRequest) {
                            HttpRequest httpRequest = (HttpRequest) httpObject;
                            httpRequest.setUri(httpRequest.uri().replace("/originalrequest", "/modifyrequest"));
                        }

                        return super.clientToProxyRequest(httpObject);
                    }
                };
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/originalrequest"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testRequestFilterCanModifyHttpRequestBody() throws IOException {
        final String originalText = "original body";
        final String newText = "modified body";

        String url = "/modifyrequest";
        stubFor(put(urlMatching(url)).
                withRequestBody(WireMock.equalTo(newText)).
                willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addRequestFilter(new RequestFilter() {
            @Override
            public HttpResponse filterRequest(HttpRequest request, HttpMessageContents contents, HttpMessageInfo messageInfo) {
                if (contents.isText()) {
                    if (contents.getTextContents().equals(originalText)) {
                        contents.setTextContents(newText);
                    }
                }

                return null;
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpPut request = new HttpPut("http://localhost:" + mockServerPort + "/modifyrequest");
            request.setEntity(new StringEntity(originalText));
            CloseableHttpResponse response = httpClient.execute(request);
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);

            verify(1, putRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testRequestFilterCanModifyHttpsRequestBody() throws IOException {
        final String originalText = "original body";
        final String newText = "modified body";

        String url = "/modifyrequest";
        stubFor(put(urlMatching(url)).
                withRequestBody(WireMock.equalTo(newText)).
                willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (contents.isText()) {
                if (contents.getTextContents().equals(originalText)) {
                    contents.setTextContents(newText);
                }
            }

            return null;
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpPut request = new HttpPut("https://localhost:" + mockServerHttpsPort + "/modifyrequest");
            request.setEntity(new StringEntity(originalText));
            CloseableHttpResponse response = httpClient.execute(request);
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);

            verify(1, putRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testResponseFilterCanModifyBinaryContents() throws IOException {
        final byte[] originalBytes = new byte[]{1, 2, 3, 4, 5};
        final byte[] newBytes = new byte[]{20, 30, 40, 50, 60};

        String url = "/modifyresponse";
        stubFor(
                get(urlEqualTo(url)).
                        willReturn(ok().
                                withBody(originalBytes).
                                withHeader(HttpHeaders.Names.CONTENT_TYPE, "application/octet-stream")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            if (!contents.isText()) {
                if (Arrays.equals(originalBytes, contents.getBinaryContents())) {
                    contents.setBinaryContents(newBytes);
                }
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpGet request = new HttpGet("http://localhost:" + mockServerPort + "/modifyresponse");
            CloseableHttpResponse response = httpClient.execute(request);
            byte[] responseBytes = ByteStreams.toByteArray(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertThat("Did not receive expected response from mock server", responseBytes, equalTo(newBytes));

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testResponseFilterCanModifyHttpTextContents() throws IOException {
        final String originalText = "The quick brown fox jumps over the lazy dog";
        final String newText = "The quick brown fox jumped.";

        String url = "/modifyresponse";
        stubFor(
                get(urlEqualTo(url)).
                        willReturn(ok().
                                withBody(originalText).
                                withHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            if (contents.isText()) {
                if (contents.getTextContents().equals(originalText)) {
                    contents.setTextContents(newText);
                }
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpGet request = new HttpGet("http://localhost:" + mockServerPort + "/modifyresponse");
            request.addHeader("Accept-Encoding", "gzip");
            CloseableHttpResponse response = httpClient.execute(request);
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", newText, responseBody);

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testResponseFilterCanModifyHttpsTextContents() throws IOException {
        final String originalText = "The quick brown fox jumps over the lazy dog";
        final String newText = "The quick brown fox jumped.";

        String url = "/modifyresponse";
        stubFor(
                get(urlEqualTo(url)).
                        willReturn(ok().
                                withBody(originalText).
                                withHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            if (contents.isText()) {
                if (contents.getTextContents().equals(originalText)) {
                    contents.setTextContents(newText);
                }
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpGet request = new HttpGet("https://localhost:" + mockServerHttpsPort + "/modifyresponse");
            request.addHeader("Accept-Encoding", "gzip");
            CloseableHttpResponse response = httpClient.execute(request);
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", newText, responseBody);

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testResponseInterceptorWithoutBody() throws IOException {
        String url = "/interceptortest";
        stubFor(
                head(urlMatching(url)).
                        willReturn(ok().
                                withHeader(HttpHeaders.Names.CONTENT_TYPE, "application/octet-stream")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicReference<byte[]> responseContents = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> responseContents.set(contents.getBinaryContents()));

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpHead("http://localhost:" + mockServerPort + "/interceptortest"));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Expected binary contents captured in interceptor to be empty", 0, responseContents.get().length);

            verify(1, headRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testResponseFilterOriginalRequestNotModified() throws IOException {
        String url = "/modifiedendpoint";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (request.uri().endsWith("/originalendpoint")) {
                request.setUri(request.uri().replaceAll("originalendpoint", "modifiedendpoint"));
            }

            return null;
        });

        final AtomicReference<String> originalRequestUri = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> originalRequestUri.set(messageInfo.getOriginalRequest().uri()));

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/originalendpoint"));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertThat("Expected URI on originalRequest to match actual URI of original HTTP request", originalRequestUri.get(), endsWith("/originalendpoint"));

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testMessageContentsNotAvailableWithoutAggregation() throws IOException {
        String url = "/endpoint";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicBoolean requestContentsNull = new AtomicBoolean(false);
        final AtomicBoolean responseContentsNull = new AtomicBoolean(false);

        proxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource((request, contents, messageInfo) -> {
            if (contents == null) {
                requestContentsNull.set(true);
            }

            return null;
        }, 0));

        proxy.addFirstHttpFilterFactory(new ResponseFilterAdapter.FilterSource(new ResponseFilter() {
            @Override
            public void filterResponse(HttpResponse response, HttpMessageContents contents, HttpMessageInfo messageInfo) {
                if (contents == null) {
                    responseContentsNull.set(true);
                }
            }
        }, 0));

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/endpoint"));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertTrue("Expected HttpMessageContents to be null in RequestFilter because HTTP message aggregation is disabled", requestContentsNull.get());
            assertTrue("Expected HttpMessageContents to be null in ResponseFilter because HTTP message aggregation is disabled", responseContentsNull.get());

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testMitmDisabledHttpsRequestFilterNotAvailable() throws IOException {
        String url = "/mitmdisabled";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setMitmDisabled(true);

        proxy.start();

        final AtomicBoolean connectRequestFilterFired = new AtomicBoolean(false);
        final AtomicBoolean getRequestFilterFired = new AtomicBoolean(false);

        proxy.addRequestFilter(new RequestFilter() {
            @Override
            public HttpResponse filterRequest(HttpRequest request, HttpMessageContents contents, HttpMessageInfo messageInfo) {
                if (request.getMethod().equals(HttpMethod.CONNECT)) {
                    connectRequestFilterFired.set(true);
                } else if (request.getMethod().equals(HttpMethod.GET)) {
                    getRequestFilterFired.set(true);
                }
                return null;
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/mitmdisabled"));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());

            assertTrue("Expected request filter to fire on CONNECT", connectRequestFilterFired.get());
            assertFalse("Expected request filter to fail to fire on GET because MITM is disabled", getRequestFilterFired.get());

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testMitmDisabledHttpsResponseFilterNotAvailable() throws IOException {
        String url = "/mitmdisabled";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setMitmDisabled(true);

        proxy.start();

        // unlike the request filter, the response filter doesn't fire when the 200 response to the CONNECT is sent to the client.
        // this is because the response filter is triggered when the serverToProxyResponse() filtering method is called, and
        // the "200 Connection established" is generated by the proxy itself.

        final AtomicBoolean responseFilterFired = new AtomicBoolean(false);

        proxy.addResponseFilter(new ResponseFilter() {
            @Override
            public void filterResponse(HttpResponse response, HttpMessageContents contents, HttpMessageInfo messageInfo) {
                responseFilterFired.set(true);
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/mitmdisabled"));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertFalse("Expected response filter to fail to fire because MITM is disabled", responseFilterFired.get());

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    /**
     * Helper method for executing response modification tests.
     */
    private void testModifiedResponse(final String originalText, final String newText) throws IOException {
        String url = "/modifyresponse";
        stubFor(
                get(urlMatching(url)).
                        willReturn(ok().
                                withBody(originalText).
                                withHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addFirstHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpObject proxyToClientResponse(HttpObject httpObject) {
                        if (httpObject instanceof FullHttpResponse) {
                            FullHttpResponse httpResponseAndContent = (FullHttpResponse) httpObject;

                            String bodyContent = HttpObjectUtil.extractHttpEntityBody(httpResponseAndContent);

                            if (bodyContent.equals(originalText)) {
                                HttpObjectUtil.replaceTextHttpEntityBody(httpResponseAndContent, newText);
                            }
                        }

                        return super.proxyToClientResponse(httpObject);
                    }
                };
            }

            @Override
            public int getMaximumResponseBufferSizeInBytes() {
                return 10000;
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/modifyresponse"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", newText, responseBody);

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testCanBypassFilterForRequest() throws IOException, InterruptedException {
        String url = "/bypassfilter";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicInteger filtersSourceHitCount = new AtomicInteger();
        final AtomicInteger filterHitCount = new AtomicInteger();

        proxy.addFirstHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                if (filtersSourceHitCount.getAndIncrement() == 0) {
                    return null;
                } else {
                    return new HttpFiltersAdapter(originalRequest) {
                        @Override
                        public void serverToProxyResponseReceived() {
                            filterHitCount.incrementAndGet();
                        }
                    };
                }
            }
        });

        // during the first request, the filterRequest(...) method should return null, which will prevent the filter instance from
        // being added to the filter chain
        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/bypassfilter"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);

        assertEquals("Expected filters source to be invoked on first request", 1, filtersSourceHitCount.get());
        assertEquals("Expected filter instance to be bypassed on first request", 0, filterHitCount.get());

        // during the second request, the filterRequest(...) method will return a filter instance, which should be invoked during processing
        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/bypassfilter"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);

        assertEquals("Expected filters source to be invoked again on second request", 2, filtersSourceHitCount.get());
        assertEquals("Expected filter instance to be invoked on second request (only)", 1, filterHitCount.get());

        verify(2, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testHttpResponseFilterMessageInfoPopulated() throws IOException {
        String urlPattern = "/httpmessageinfopopulated.*";
        stubFor(
                get(urlMatching(urlPattern)).
                        withQueryParam("param1", WireMock.equalTo("value1")).
                        willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicReference<ChannelHandlerContext> requestCtx = new AtomicReference<>();
        final AtomicReference<HttpRequest> requestOriginalRequest = new AtomicReference<>();
        final AtomicBoolean requestIsHttps = new AtomicBoolean(false);
        final AtomicReference<String> requestFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> requestFilterUrl = new AtomicReference<>();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            requestCtx.set(messageInfo.getChannelHandlerContext());
            requestOriginalRequest.set(messageInfo.getOriginalRequest());
            requestIsHttps.set(messageInfo.isHttps());
            requestFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            requestFilterUrl.set(messageInfo.getUrl());
            return null;
        });

        final AtomicReference<ChannelHandlerContext> responseCtx = new AtomicReference<>();
        final AtomicReference<HttpRequest> responseOriginalRequest = new AtomicReference<>();
        final AtomicBoolean responseIsHttps = new AtomicBoolean(false);
        final AtomicReference<String> responseFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> responseFilterUrl = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            responseCtx.set(messageInfo.getChannelHandlerContext());
            responseOriginalRequest.set(messageInfo.getOriginalRequest());
            responseIsHttps.set(messageInfo.isHttps());
            responseFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            responseFilterUrl.set(messageInfo.getUrl());
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String requestUrl = "http://localhost:" + mockServerPort + "/httpmessageinfopopulated?param1=value1";
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertNotNull("Expected ChannelHandlerContext to be populated in request filter", requestCtx.get());
            assertNotNull("Expected originalRequest to be populated in request filter", requestOriginalRequest.get());
            assertFalse("Expected isHttps to return false in request filter", requestIsHttps.get());
            assertEquals("Expected originalUrl in request filter to match actual request URL", requestUrl, requestFilterOriginalUrl.get());
            assertEquals("Expected url in request filter to match actual request URL", requestUrl, requestFilterUrl.get());

            assertNotNull("Expected ChannelHandlerContext to be populated in response filter", responseCtx.get());
            assertNotNull("Expected originalRequest to be populated in response filter", responseOriginalRequest.get());
            assertFalse("Expected isHttps to return false in response filter", responseIsHttps.get());
            assertEquals("Expected originalUrl in response filter to match actual request URL", requestUrl, responseFilterOriginalUrl.get());
            assertEquals("Expected url in response filter to match actual request URL", requestUrl, responseFilterUrl.get());

            verify(1, getRequestedFor(urlMatching(urlPattern)));
        }
    }

    @Test
    public void testHttpResponseFilterUrlReflectsModifications() throws IOException {
        String url = "/urlreflectsmodifications";
        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicReference<String> requestFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> requestFilterUrl = new AtomicReference<>();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            requestFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            requestFilterUrl.set(messageInfo.getUrl());
            return null;
        });

        // request filters get added to the beginning of the filter chain, so add this uri-modifying request filter after
        // adding the capturing request filter above.
        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (request.uri().endsWith("/originalurl")) {
                String newUrl = request.uri().replaceAll("originalurl", "urlreflectsmodifications");
                request.setUri(newUrl);
            }
            return null;
        });

        final AtomicReference<String> responseFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> responseFilterUrl = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            responseFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            responseFilterUrl.set(messageInfo.getUrl());
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String originalRequestUrl = "http://localhost:" + mockServerPort + "/originalurl";
            String modifiedRequestUrl = "http://localhost:" + mockServerPort + "/urlreflectsmodifications";
            CloseableHttpResponse response = httpClient.execute(new HttpGet(originalRequestUrl));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Expected originalUrl in request filter to match actual request URL", originalRequestUrl, requestFilterOriginalUrl.get());
            assertEquals("Expected url in request filter to match modified request URL", modifiedRequestUrl, requestFilterUrl.get());

            assertEquals("Expected originalUrl in response filter to match actual request URL", originalRequestUrl, responseFilterOriginalUrl.get());
            assertEquals("Expected url in response filter to match modified request URL", modifiedRequestUrl, responseFilterUrl.get());

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testHttpsResponseFilterUrlReflectsModifications() throws IOException {
        String url = "/urlreflectsmodifications";
        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        final AtomicReference<String> requestFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> requestFilterUrl = new AtomicReference<>();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            requestFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            requestFilterUrl.set(messageInfo.getUrl());
            return null;
        });

        // request filters get added to the beginning of the filter chain, so add this uri-modifying request filter after
        // adding the capturing request filter above.
        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (request.uri().endsWith("/originalurl")) {
                String newUrl = request.uri().replaceAll("originalurl", "urlreflectsmodifications");
                request.setUri(newUrl);
            }
            return null;
        });

        final AtomicReference<String> responseFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> responseFilterUrl = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            responseFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            responseFilterUrl.set(messageInfo.getUrl());
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String originalRequestUrl = "https://localhost:" + mockServerHttpsPort + "/originalurl";
            String modifiedRequestUrl = "https://localhost:" + mockServerHttpsPort + "/urlreflectsmodifications";
            CloseableHttpResponse response = httpClient.execute(new HttpGet(originalRequestUrl));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());
            assertEquals("Expected originalUrl in request filter to match actual request URL", originalRequestUrl, requestFilterOriginalUrl.get());
            assertEquals("Expected url in request filter to match modified request URL", modifiedRequestUrl, requestFilterUrl.get());

            assertEquals("Expected originalUrl in response filter to match actual request URL", originalRequestUrl, responseFilterOriginalUrl.get());
            assertEquals("Expected url in response filter to match modified request URL", modifiedRequestUrl, responseFilterUrl.get());

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    public void testHttpsResponseFilterMessageInfoPopulated() throws IOException {
        String urlPattern = "/httpmessageinfopopulated.*";
        stubFor(
                get(urlMatching(urlPattern)).
                        withQueryParam("param1", WireMock.equalTo("value1")).
                        willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        final AtomicReference<ChannelHandlerContext> requestCtx = new AtomicReference<>();
        final AtomicReference<HttpRequest> requestOriginalRequest = new AtomicReference<>();
        final AtomicBoolean requestIsHttps = new AtomicBoolean(false);
        final AtomicReference<String> requestOriginalUrl = new AtomicReference<>();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            requestCtx.set(messageInfo.getChannelHandlerContext());
            requestOriginalRequest.set(messageInfo.getOriginalRequest());
            requestIsHttps.set(messageInfo.isHttps());
            requestOriginalUrl.set(messageInfo.getOriginalUrl());
            return null;
        });

        final AtomicReference<ChannelHandlerContext> responseCtx = new AtomicReference<>();
        final AtomicReference<HttpRequest> responseOriginalRequest = new AtomicReference<>();
        final AtomicBoolean responseIsHttps = new AtomicBoolean(false);
        final AtomicReference<String> responseOriginalUrl = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            responseCtx.set(messageInfo.getChannelHandlerContext());
            responseOriginalRequest.set(messageInfo.getOriginalRequest());
            responseIsHttps.set(messageInfo.isHttps());
            responseOriginalUrl.set(messageInfo.getOriginalUrl());
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String requestUrl = "https://localhost:" + mockServerHttpsPort + "/httpmessageinfopopulated?param1=value1";
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));

            assertEquals("Expected server to return a 200", 200, response.getStatusLine().getStatusCode());

            assertNotNull("Expected ChannelHandlerContext to be populated in request filter", requestCtx.get());
            assertNotNull("Expected originalRequest to be populated in request filter", requestOriginalRequest.get());
            assertTrue("Expected isHttps to return true in request filter", requestIsHttps.get());
            assertEquals("Expected originalUrl in request filter to match actual request URL", requestUrl, requestOriginalUrl.get());

            assertNotNull("Expected ChannelHandlerContext to be populated in response filter", responseCtx.get());
            assertNotNull("Expected originalRequest to be populated in response filter", responseOriginalRequest.get());
            assertTrue("Expected isHttps to return true in response filter", responseIsHttps.get());
            assertEquals("Expected originalUrl in response filter to match actual request URL", requestUrl, responseOriginalUrl.get());

            verify(1, getRequestedFor(urlMatching(urlPattern)));
        }
    }
}
