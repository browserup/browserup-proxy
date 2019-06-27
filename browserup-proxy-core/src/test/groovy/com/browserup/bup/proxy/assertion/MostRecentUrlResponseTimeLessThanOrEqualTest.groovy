package com.browserup.bup.proxy.assertion


import org.hamcrest.Matchers
import org.junit.Test

import java.util.regex.Pattern

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static org.junit.Assert.*

class MostRecentUrlResponseTimeLessThanOrEqualTest extends BaseAssertionsTest {

    @Test
    void mostRecentUrlResponseTimeExceeds() {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY)

        requestToMockedServer(URL_PATH)

        def assertionTime = DEFAULT_RESPONSE_DELAY - TIME_DELTA_MILLISECONDS

        def result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected failed flag to be true", result.failed)
        assertFalse("Expected passed flag to be true", result.passed)

        verify(1, getRequestedFor(urlMatching(".*${URL_PATH}.*")))
    }

    @Test
    void passesIfNoEntriesFound() {
        mockResponseForPathWithDelay(URL_PATH, 0)

        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile("^does not match?"), 0)

        assertTrue("Expected passed flag to be true", result.passed)
        assertFalse("Expected failed flag to be true", result.failed)
        assertThat("Expected to get one har entry result", result.requests, Matchers.empty())

        verify(1, getRequestedFor(urlMatching(".*${URL_PATH}.*")))
    }

    @Test
    void mostRecentUrlResponseTimeLessThanOrEqual() {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY)

        requestToMockedServer(URL_PATH)

        def assertionTime = DEFAULT_RESPONSE_DELAY + TIME_DELTA_MILLISECONDS

        def result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile(".*${URL_PATH}.*"), assertionTime)

        assertTrue("Expected passed flag to be true", result.passed)
        assertFalse("Expected failed flag to be false", result.failed)
        assertThat("Expected to get one har entry result", result.requests, Matchers.hasSize(1))

        verify(1, getRequestedFor(urlMatching(".*${URL_PATH}.*")))
    }
}
