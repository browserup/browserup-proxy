package com.browserup.bup.proxy.assertion

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.CaptureType
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

abstract class BaseAssertionsTest extends MockServerTest {
    protected static final String SUCCESSFUL_RESPONSE_BODY = "success"
    protected static final String URL_PATH = "some-url"
    protected static final Pattern URL_PATH_PATTERN = Pattern.compile(".*${URL_PATH}.*")
    protected static final Delay DEFAULT_RESPONSE_DELAY = new Delay(TimeUnit.SECONDS, 2)
    protected static final Delay FAST_RESPONSE_DELAY = new Delay(TimeUnit.SECONDS, 1)
    protected static final int TIME_DELTA_MILLISECONDS = 100

    protected String url
    protected String mockedServerUrl

    protected BrowserUpProxy proxy
    protected CloseableHttpClient clientToProxy

    void requestToMockedServer(String url, String response=SUCCESSFUL_RESPONSE_BODY) {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet("${mockedServerUrl}/${url}".toString())).entity.content)
        assertEquals("Did not receive expected response from mock server", response, respBody)
    }

    @Before
    void startUp() {
        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        proxy.newHar()

        clientToProxy = NewProxyServerTestUtil.getNewHttpClient(proxy.port)
        mockedServerUrl = "http://localhost:${mockServerPort}"
        url = "${mockedServerUrl}/${URL_PATH}"
    }

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    protected mockResponseForPathWithDelay(String path, Delay delay) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${path}"),
                Times.once())
                .respond(response()
                .withStatusCode(HttpStatus.SC_OK)
                .withDelay(delay)
                .withBody(SUCCESSFUL_RESPONSE_BODY))
    }

    static def assertAssertionPassed(AssertionResult assertion) {
        assertTrue("Expected assertion to pass", assertion.passed)
        assertFalse("Expected assertion to pass", assertion.failed)
    }

    static def assertAssertionFailed(AssertionResult assertion) {
        assertFalse("Expected assertion to fail", assertion.passed)
        assertTrue("Expected assertion to fail", assertion.failed)
    }

    static def assertAssertionHasNoEntries(AssertionResult assertion) {
        assertThat('Expected assertion result has no entries', assertion.requests, Matchers.hasSize(0))
    }
}
