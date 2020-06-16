/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.filters

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.google.common.collect.ImmutableList
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.RewriteRule
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test

import java.nio.channels.Channel

import static com.browserup.bup.filters.HttpsAwareFiltersAdapter.*
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@org.junit.Ignore
class RewriteUrlFilterTest extends MockServerTest {
    BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testRewriteWithCaptureGroups() {
        HttpHeaders mockHeaders = mock(HttpHeaders.class)
        when(mockHeaders.contains(HttpHeaderNames.HOST)).thenReturn(false)

        HttpRequest request = mock(HttpRequest.class)
        when(request.uri()).thenReturn('http://www.yahoo.com?param=someValue')
        when(request.headers()).thenReturn(mockHeaders)

        Collection<RewriteRule> rewriteRules = ImmutableList.of(new RewriteRule('http://www\\.(yahoo|bing)\\.com\\?(\\w+)=(\\w+)', 'http://www.google.com?originalDomain=$1&$2=$3'))

        // mock out the netty ChannelHandlerContext for the isHttps() call in the filter
        Attribute<Boolean> mockIsHttpsAttribute = mock(Attribute)
        when(mockIsHttpsAttribute.get()).thenReturn(Boolean.FALSE)

        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext)
        io.netty.channel.Channel channelMock = mock(io.netty.channel.Channel)
        when(mockCtx.channel()).thenReturn(channelMock)
        when(channelMock.attr(AttributeKey.<Boolean>valueOf(IS_HTTPS_ATTRIBUTE_NAME))).thenReturn(mockIsHttpsAttribute)

        RewriteUrlFilter filter = new RewriteUrlFilter(request, mockCtx, rewriteRules)
        filter.clientToProxyRequest(request)

        verify(request).setUri('http://www.google.com?originalDomain=yahoo&param=someValue')
    }

    @Test
    void testRewriteMultipleMatches() {
        HttpHeaders mockHeaders = mock(HttpHeaders.class)
        when(mockHeaders.contains(HttpHeaderNames.HOST)).thenReturn(false)

        HttpRequest request = mock(HttpRequest.class)
        when(request.uri()).thenReturn('http://www.yahoo.com?param=someValue')
        when(request.headers()).thenReturn(mockHeaders)

        Collection<RewriteRule> rewriteRules = ImmutableList.of(
                new RewriteRule('http://www\\.yahoo\\.com\\?(\\w+)=(\\w+)', 'http://www.bing.com?new$1=new$2'),
                new RewriteRule('http://www\\.(yahoo|bing)\\.com\\?(\\w+)=(\\w+)', 'http://www.google.com?originalDomain=$1&$2=$3')
        )

        // mock out the netty ChannelHandlerContext for the isHttps() call in the filter
        Attribute<Boolean> mockIsHttpsAttribute = mock(Attribute)
        when(mockIsHttpsAttribute.get()).thenReturn(Boolean.FALSE)

        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext)
        io.netty.channel.Channel channelMock = mock(io.netty.channel.Channel)
        when(mockCtx.channel()).thenReturn(channelMock)
        when(channelMock.attr(AttributeKey.<Boolean>valueOf(IS_HTTPS_ATTRIBUTE_NAME))).thenReturn(mockIsHttpsAttribute)

        RewriteUrlFilter filter = new RewriteUrlFilter(request, mockCtx, rewriteRules)
        filter.clientToProxyRequest(request)

        verify(request).setUri('http://www.google.com?originalDomain=bing&newparam=newsomeValue')
    }

    @Test
    void testRewriteHttpHost() {
        def stubUrl = "/testRewriteHttpHost"
        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Host",new EqualToPattern("localhost:${mockServerPort}"))
                .willReturn(ok()
                .withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.rewriteUrl('http://www\\.someotherhost\\.com:(\\d+)/(\\w+)', 'http://localhost:$1/$2')

        proxy.start()

        String url = "http://www.someotherhost.com:${mockServerPort}/testRewriteHttpHost"
        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse firstResponse = it.execute(new HttpGet(url))
            assertEquals("Did not receive HTTP 200 from mock server", 200, firstResponse.getStatusLine().getStatusCode())

            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(firstResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody)

            CloseableHttpResponse secondResponse = it.execute(new HttpGet(url))
            assertEquals("Did not receive HTTP 200 from mock server", 200, secondResponse.getStatusLine().getStatusCode())

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(secondResponse.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody)
        }

        WireMock.verify(2, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testRewriteHttpResource() {
        def stubUrl = "/rewrittenresource"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.rewriteUrl('http://badhost:(\\d+)/badresource', 'http://localhost:$1/rewrittenresource')

        proxy.start()

        String url = "http://badhost:${mockServerPort}/badresource"
        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(url)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody)

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(url)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody)
        }

        WireMock.verify(2, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testRewriteHttpsResource() {
        def stubUrl = "/rewrittenresource"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success")))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.rewriteUrl('https://localhost:(\\d+)/badresource', 'https://localhost:$1/rewrittenresource')

        proxy.start()

        String url = "https://localhost:${mockServerHttpsPort}/badresource"
        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(url)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody)

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(url)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody)
        }

        WireMock.verify(2, getRequestedFor(urlEqualTo(stubUrl)))
    }
}
