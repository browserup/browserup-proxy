/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.filters.util.HarCaptureUtil
import com.browserup.bup.proxy.dns.AdvancedHostResolver
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.browserup.harreader.model.*
import com.github.tomakehurst.wiremock.client.WireMock
import com.google.common.collect.Iterables
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class HarValidationTest extends MockServerTest {
    private BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testDefaultValuesOfHarResponse() {
        def stubUrl = "/testUrl.*"
        stubFor(get(urlMatching(stubUrl)).willReturn(ok()))

        proxy = new BrowserUpProxyServer()
        proxy.start()

        proxy.newHar()

        def requestUrl = "http://localhost:${mockServerPort}/testUrl"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(requestUrl)).getEntity().getContent())
        }

        Thread.sleep(500)
        def har = proxy.getHar()
        def entry = proxy.har.log.entries.get(0)
        def harResponse = entry.response

        assertEquals("Expected redirectURL to have default empty string value", "", harResponse.redirectURL)
        assertEquals("Expected content size to have value 0 by default", 0, harResponse.content.size)
        assertEquals("Expected empty mime type", "", har.log.entries[0].response.content.mimeType)
        assertEquals("Expected empty request post data mime type", "", har.log.entries[0].request.postData.mimeType)

        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }


}
