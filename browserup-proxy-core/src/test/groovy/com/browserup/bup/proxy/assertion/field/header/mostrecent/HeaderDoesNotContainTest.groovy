package com.browserup.bup.proxy.assertion.field.header.mostrecent

import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class HeaderDoesNotContainTest extends HeaderBaseTest {
    private static final Pattern URL_PATTERN = Pattern.compile(".*${URL_PATH}.*")

    @Test
    void anyNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, NOT_MATCHING_HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void nameNotProvidedAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, null, NOT_MATCHING_HEADER_VALUE)

        assertAssertionPassed(result)

        result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, '', NOT_MATCHING_HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void nameNotProvidedAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, null, HEADER_VALUE)

        assertAssertionFailed(result)

        result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, '', HEADER_VALUE)

        assertAssertionFailed(result)
    }

    @Test
    void anyNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_VALUE)

        assertAssertionFailed(result)
    }

    @Test
    void matchingNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_NAME, NOT_MATCHING_HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void notMatchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, NOT_MATCHING_HEADER_NAME, HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void matchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_NAME, HEADER_VALUE)

        assertAssertionFailed(result)
    }
}
