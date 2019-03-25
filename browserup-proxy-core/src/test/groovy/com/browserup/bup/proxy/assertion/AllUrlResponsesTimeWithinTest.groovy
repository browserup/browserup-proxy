package com.browserup.bup.proxy.assertion


import org.apache.http.client.methods.HttpGet
import org.junit.Test

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.junit.Assert.*

class AllUrlResponsesTimeWithinTest extends BaseAssertionsTest {

    @Test
    void allUrlResponsesTimeExceeds() {
        def range = (1..3)
        range.forEach {
            mockResponseForPathWithDelay(URL_PATH + it, DEFAULT_RESPONSE_DELAY)
        }

        range.forEach {
            def url = "http://localhost:${mockServerPort}/${URL_PATH + it}"

            def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
            assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)
        }


        def assertionTime = MILLISECONDS.convert(DEFAULT_RESPONSE_DELAY.value, DEFAULT_RESPONSE_DELAY.timeUnit) - TIME_DELTA_MILLISECONDS

        def result = proxy.assertAllUrlResponseTimesWithin(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected failed flag to be true", result.failed)
        assertFalse("Expected passed flag to be true", result.passed)

        result.requests.forEach {
            assertTrue("Expected entry result to have failed flag = true", it.failed)
        }
    }

    @Test
    void someUrlResponsesTimeExceeds() {
        def fastRange = (1..2)
        fastRange.forEach {
            mockResponseForPathWithDelay(URL_PATH + it, FAST_RESPONSE_DELAY)
        }

        def slowRange = (3..4)
        slowRange.forEach {
            mockResponseForPathWithDelay(URL_PATH + it, DEFAULT_RESPONSE_DELAY)
        }

        fastRange.forEach {
            def url = "http://localhost:${mockServerPort}/${URL_PATH + it}"

            def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
            assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)
        }

        slowRange.forEach {
            def url = "http://localhost:${mockServerPort}/${URL_PATH + it}"

            def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
            assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)
        }

        def assertionTime = MILLISECONDS.convert(DEFAULT_RESPONSE_DELAY.value, DEFAULT_RESPONSE_DELAY.timeUnit) - TIME_DELTA_MILLISECONDS

        def result = proxy.assertAllUrlResponseTimesWithin(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected failed flag to be true", result.failed)
        assertFalse("Expected passed flag to be true", result.passed)

        result.requests.forEach { e ->
            if (fastRange.find {e.url.contains "${URL_PATH + it}"}) {
                assertFalse("Expected entry result for fast response to have failed flag = false",e.failed)
            }
            if (slowRange.find {e.url.contains "${URL_PATH + it}"}) {
                assertTrue("Expected entry result for slow response to have failed flag = true",e.failed)
            }
        }
    }
}
