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

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)
    }

    @Test
    void nameNotProvidedAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, null, NOT_MATCHING_HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)

        result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, '', NOT_MATCHING_HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)
    }

    @Test
    void nameNotProvidedAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, null, HEADER_VALUE)

        assertFalse("Expected to find header value", result.passed)
        assertTrue("Expected to find header value", result.failed)

        result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, '', HEADER_VALUE)

        assertFalse("Expected to find header value", result.passed)
        assertTrue("Expected to find header value", result.failed)
    }

    @Test
    void anyNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_VALUE)

        assertFalse("Expected to find header value", result.passed)
        assertTrue("Expected to find header value", result.failed)
    }

    @Test
    void matchingNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_NAME, NOT_MATCHING_HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)
    }

    @Test
    void notMatchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, NOT_MATCHING_HEADER_NAME, HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)
    }

    @Test
    void matchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_NAME, HEADER_VALUE)

        assertFalse("Expected to find header value", result.passed)
        assertTrue("Expected to find header value", result.failed)
    }
}
