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
import org.hamcrest.Matcher
import org.hamcrest.core.Every
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

@org.junit.Ignore
class HarValidationTest extends MockServerTest {
    private BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testDefaultValuesOfMockedHarResponse() {
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

        assertNotNull("Expected not null log creator name", har.log.creator.name)
        assertNotNull("Expected not null log creator version", har.log.creator.version)

        har.log.pages.each {
            assertNotNull("Expected not null har log pages id", it.id)
            assertNotNull("Expected not null har log pages title", it.title)
            assertNotNull("Expected not null har log pages startedDateTime", it.startedDateTime)
            assertNotNull("Expected not null har log pages pageTimings", it.pageTimings)
        }

        har.log.entries.each {
            assertNotNull("Expected not null har entries startedDateTime", it.startedDateTime)
            assertNotNull("Expected not null har entries time", it.time)
            assertNotNull("Expected not null har entries request", it.request)
            assertNotNull("Expected not null har entries response", it.response)
            assertNotNull("Expected not null har entries cache", it.cache)
            assertNotNull("Expected not null har entries timings", it.timings)

            assertNotNull("Expected not null har entries requests method", it.request.method)
            assertNotNull("Expected not null har entries requests url", it.request.url)
            assertNotNull("Expected not null har entries requests httpVersion", it.request.httpVersion)
            assertNotNull("Expected not null har entries requests cookies", it.request.cookies)
            assertNotNull("Expected not null har entries requests headers", it.request.headers)
            assertNotNull("Expected not null har entries requests queryString", it.request.queryString)
            assertNotNull("Expected not null har entries requests headersSize", it.request.headersSize)
            assertNotNull("Expected not null har entries requests bodySize", it.request.bodySize)

            assertNotNull("Expected not null har entries responses status", it.response.status)
            assertNotNull("Expected not null har entries responses statusText", it.response.statusText)
            assertNotNull("Expected not null har entries responses httpVersion", it.response.httpVersion)
            assertNotNull("Expected not null har entries responses cookies", it.response.cookies)
            assertNotNull("Expected not null har entries responses content", it.response.content)
            assertNotNull("Expected not null har entries responses redirectURL", it.response.redirectURL)
            assertNotNull("Expected not null har entries responses headersSize", it.response.headersSize)
            assertNotNull("Expected not null har entries responses bodySize", it.response.bodySize)

            it.response.cookies.each { cookie ->
                assertNotNull("Expected not null har entries responses cookies name", cookie.name)
                assertNotNull("Expected not null har entries responses cookies value", cookie.value)
            }

            assertNotNull("Expected not null har entries requests postData mimeType", it.request.postData.mimeType)
            assertNotNull("Expected not null har entries requests postData params", it.request.postData.params)
            assertNotNull("Expected not null har entries requests postData text", it.request.postData.text)

            it.request.postData.params.each { param ->
                assertNotNull("Expected not null har entries requests postData params name", param.name)
            }

            assertNotNull("Expected not null har entries responses content size", it.response.content.size)
            assertNotNull("Expected not null har entries responses content mimeType", it.response.content.mimeType)
            assertNotNull("Expected not null har entries responses content text", it.response.content.text)

            assertNotNull("Expected not null har entries timings send", it.timings.send)
            assertNotNull("Expected not null har entries timings wait", it.timings.wait)
            assertNotNull("Expected not null har entries timings receive", it.timings.receive)
        }
        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testDefaultValuesOfDefaultHar() {
        def har = new Har()
        def harLog = new HarLog()
        def harPage = new HarPage()
        def harEntry = new HarEntry()
        def harResponse = new HarResponse()
        def harRequest = new HarRequest()
        def harPostData = new HarPostData()
        harPostData.params = Collections.singletonList(new HarPostDataParam())

        harResponse.cookies = Collections.singletonList(new HarCookie())
        harRequest.postData = harPostData
        harEntry.request = harRequest
        harEntry.response = harResponse
        harLog.creator = new HarCreatorBrowser()
        harLog.pages = Collections.singletonList(harPage)
        harLog.entries = Collections.singletonList(harEntry)
        har.log = harLog

        assertNotNull("Expected not null log creator name", har.log.creator.name)
        assertNotNull("Expected not null log creator version", har.log.creator.version)

        har.log.pages.each {
            assertNotNull("Expected not null har log pages id", it.id)
            assertNotNull("Expected not null har log pages title", it.title)
            assertNotNull("Expected not null har log pages startedDateTime", it.startedDateTime)
            assertNotNull("Expected not null har log pages pageTimings", it.pageTimings)
        }

        har.log.entries.each {
            assertNotNull("Expected not null har entries startedDateTime", it.startedDateTime)
            assertNotNull("Expected not null har entries time", it.time)
            assertNotNull("Expected not null har entries request", it.request)
            assertNotNull("Expected not null har entries response", it.response)
            assertNotNull("Expected not null har entries cache", it.cache)
            assertNotNull("Expected not null har entries timings", it.timings)

            assertNotNull("Expected not null har entries requests method", it.request.method)
            assertNotNull("Expected not null har entries requests url", it.request.url)
            assertNotNull("Expected not null har entries requests httpVersion", it.request.httpVersion)
            assertNotNull("Expected not null har entries requests cookies", it.request.cookies)
            assertNotNull("Expected not null har entries requests headers", it.request.headers)
            assertNotNull("Expected not null har entries requests queryString", it.request.queryString)
            assertNotNull("Expected not null har entries requests headersSize", it.request.headersSize)
            assertNotNull("Expected not null har entries requests bodySize", it.request.bodySize)

            assertNotNull("Expected not null har entries responses status", it.response.status)
            assertNotNull("Expected not null har entries responses statusText", it.response.statusText)
            assertNotNull("Expected not null har entries responses httpVersion", it.response.httpVersion)
            assertNotNull("Expected not null har entries responses cookies", it.response.cookies)
            assertNotNull("Expected not null har entries responses content", it.response.content)
            assertNotNull("Expected not null har entries responses redirectURL", it.response.redirectURL)
            assertNotNull("Expected not null har entries responses headersSize", it.response.headersSize)
            assertNotNull("Expected not null har entries responses bodySize", it.response.bodySize)

            it.response.cookies.each { cookie ->
                assertNotNull("Expected not null har entries responses cookies name", cookie.name)
                assertNotNull("Expected not null har entries responses cookies value", cookie.value)
            }

            assertNotNull("Expected not null har entries requests postData mimeType", it.request.postData.mimeType)
            assertNotNull("Expected not null har entries requests postData params", it.request.postData.params)
            assertNotNull("Expected not null har entries requests postData text", it.request.postData.text)

            it.request.postData.params.each { param ->
                assertNotNull("Expected not null har entries requests postData params name", param.name)
            }

            assertNotNull("Expected not null har entries responses content size", it.response.content.size)
            assertNotNull("Expected not null har entries responses content mimeType", it.response.content.mimeType)
            assertNotNull("Expected not null har entries responses content text", it.response.content.text)

            assertNotNull("Expected not null har entries timings send", it.timings.send)
            assertNotNull("Expected not null har entries timings wait", it.timings.wait)
            assertNotNull("Expected not null har entries timings receive", it.timings.receive)
        }
    }
}
