/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Delay

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class AssertionsTest extends MockServerTest {
    private static final String SUCCESSFUL_RESPONSE_BODY = "success"
    private static final String URL_PATH = "some-url"
    private static final Delay DEFAULT_RESPONSE_DELAY = new Delay(TimeUnit.SECONDS, 2)
    private static final int TIME_DELTA_MILLISECONDS = 100

    private BrowserUpProxy proxy
    private CloseableHttpClient clientToProxy

    @Before
    void startUp() {
        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        proxy.newHar()

        clientToProxy = NewProxyServerTestUtil.getNewHttpClient(proxy.port)
    }

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testUrlResponseTimeExceeds() {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY)

        def url = "http://localhost:${mockServerPort}/${URL_PATH}"

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def assertionTime = MILLISECONDS.convert(DEFAULT_RESPONSE_DELAY.value, DEFAULT_RESPONSE_DELAY.timeUnit) - TIME_DELTA_MILLISECONDS

        def result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected failed flag to be true", result.failed)
        assertFalse("Expected passed flag to be true", result.passed)
    }

    @Test
    void testNoUrlResponseFoundForAssertion() {
        mockResponseForPathWithDelay(URL_PATH, new Delay(MILLISECONDS, 0))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        proxy.newHar()

        def client = NewProxyServerTestUtil.getNewHttpClient(proxy.port)

        def url = "http://localhost:${mockServerPort}/${URL_PATH}"

        def respBody = toStringAndClose(client.execute(new HttpGet(url)).getEntity().getContent())
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertMostRecentResponseContentLengthLessThanOrEqual(Pattern.compile("^does not match?"), 0)

        assertTrue("Expected passed flag to be true", result.passed)
        assertFalse("Expected failed flag to be true", result.failed)
        assertThat("Expected to get one har entry result", result.requests, Matchers.empty())
    }

    @Test
    void testUrlResponseTimeDoesNotExceed() {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY)

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        proxy.newHar()

        def client = NewProxyServerTestUtil.getNewHttpClient(proxy.port)

        def requestUrlToPassFilter = "http://localhost:${mockServerPort}/${URL_PATH}"

        def respBody = toStringAndClose(client.execute(new HttpGet(requestUrlToPassFilter)).getEntity().getContent())
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def assertionTime = MILLISECONDS.convert(DEFAULT_RESPONSE_DELAY.value, DEFAULT_RESPONSE_DELAY.timeUnit) + TIME_DELTA_MILLISECONDS

        def result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected passed flag to be true", result.passed)
        assertFalse("Expected failed flag to be false", result.failed)
        assertThat("Expected to get one har entry result", result.requests, Matchers.hasSize(1))
    }

    private mockResponseForPathWithDelay(String path, Delay delay) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${path}"),
                Times.once())
                .respond(response()
                .withStatusCode(HttpStatus.SC_OK)
                .withDelay(delay)
                .withBody(SUCCESSFUL_RESPONSE_BODY))
    }
}
