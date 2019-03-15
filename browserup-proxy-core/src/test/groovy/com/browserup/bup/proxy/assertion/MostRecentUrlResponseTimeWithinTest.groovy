package com.browserup.bup.proxy.assertion

import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.HttpGet
import org.hamcrest.Matchers
import org.junit.Test
import org.mockserver.model.Delay

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.junit.Assert.*

class MostRecentUrlResponseTimeWithinTest extends BaseAssertionsTest {

    @Test
    void mostRecentUrlResponseTimeExceeds() {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY)

        def url = "http://localhost:${mockServerPort}/${URL_PATH}"

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def assertionTime = MILLISECONDS.convert(DEFAULT_RESPONSE_DELAY.value, DEFAULT_RESPONSE_DELAY.timeUnit) - TIME_DELTA_MILLISECONDS

        def result = proxy.assertMostRecentUrlResponseTimeWithin(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected failed flag to be true", result.failed)
        assertFalse("Expected passed flag to be true", result.passed)
    }

    @Test
    void mostRecentUrlResponseTimeWithinIfNoEntriesFound() {
        mockResponseForPathWithDelay(URL_PATH, new Delay(MILLISECONDS, 0))

        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        proxy.newHar()

        def client = NewProxyServerTestUtil.getNewHttpClient(proxy.port)

        def url = "http://localhost:${mockServerPort}/${URL_PATH}"

        def respBody = toStringAndClose(client.execute(new HttpGet(url)).getEntity().getContent())
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertMostRecentUrlResponseTimeWithin(Pattern.compile("^does not match?"), 0)

        assertTrue("Expected passed flag to be true", result.passed)
        assertFalse("Expected failed flag to be true", result.failed)
        assertThat("Expected to get one har entry result", result.requests, Matchers.empty())
    }

    @Test
    void mostRecentUrlResponseTimeWithin() {
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

        def result = proxy.assertMostRecentUrlResponseTimeWithin(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected passed flag to be true", result.passed)
        assertFalse("Expected failed flag to be false", result.failed)
        assertThat("Expected to get one har entry result", result.requests, Matchers.hasSize(1))
    }
}
