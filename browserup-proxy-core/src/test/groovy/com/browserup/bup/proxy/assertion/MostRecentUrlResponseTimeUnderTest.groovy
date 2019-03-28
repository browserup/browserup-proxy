package com.browserup.bup.proxy.assertion


import org.hamcrest.Matchers
import org.junit.Test
import org.mockserver.model.Delay

import java.util.regex.Pattern

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.junit.Assert.*

class MostRecentUrlResponseTimeUnderTest extends BaseAssertionsTest {

    @Test
    void mostRecentUrlResponseTimeExceeds() {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY)

        requestToMockedServer(URL_PATH)

        def assertionTime = MILLISECONDS.convert(DEFAULT_RESPONSE_DELAY.value, DEFAULT_RESPONSE_DELAY.timeUnit) - TIME_DELTA_MILLISECONDS

        def result = proxy.assertMostRecentResponseTimeUnder(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected failed flag to be true", result.failed)
        assertFalse("Expected passed flag to be true", result.passed)
    }

    @Test
    void passesIfNoEntriesFound() {
        mockResponseForPathWithDelay(URL_PATH, new Delay(MILLISECONDS, 0))

        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseTimeUnder(Pattern.compile("^does not match?"), 0)

        assertTrue("Expected passed flag to be true", result.passed)
        assertFalse("Expected failed flag to be true", result.failed)
        assertThat("Expected to get one har entry result", result.requests, Matchers.empty())
    }

    @Test
    void mostRecentUrlResponseTimeUnder() {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY)

        requestToMockedServer(URL_PATH)

        def assertionTime = MILLISECONDS.convert(DEFAULT_RESPONSE_DELAY.value, DEFAULT_RESPONSE_DELAY.timeUnit) + TIME_DELTA_MILLISECONDS

        def result = proxy.assertMostRecentResponseTimeUnder(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected passed flag to be true", result.passed)
        assertFalse("Expected failed flag to be false", result.failed)
        assertThat("Expected to get one har entry result", result.requests, Matchers.hasSize(1))
    }
}
