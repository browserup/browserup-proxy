package com.browserup.bup.proxy.assertion

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

abstract class BaseAssertionsTest extends MockServerTest {
    protected static final String SUCCESSFUL_RESPONSE_BODY = "success"
    protected static final String URL_PATH = "some-url"
    protected static final Pattern URL_PATH_PATTERN = Pattern.compile(".*${URL_PATH}.*")
    protected static final int DEFAULT_RESPONSE_DELAY = 2000
    protected static final int FAST_RESPONSE_DELAY = 1000
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

    protected mockResponseForPathWithDelay(String path, int delayMilliseconds) {
        stubFor(get(urlEqualTo("/" + path))
                .willReturn(ok().withFixedDelay(delayMilliseconds)
                .withBody(SUCCESSFUL_RESPONSE_BODY))
        )
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
