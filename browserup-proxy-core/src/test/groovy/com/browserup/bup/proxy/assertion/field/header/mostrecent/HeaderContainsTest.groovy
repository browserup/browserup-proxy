package com.browserup.bup.proxy.assertion.field.header.mostrecent

import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class HeaderContainsTest extends HeaderBaseTest {
    private static final Pattern URL_PATTERN = Pattern.compile(".*${URL_PATH}.*")

    @Test
    void anyNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void anyNameIfEmptyNameProvidedAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, null, HEADER_VALUE)

        assertAssertionPassed(result)

        result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, '', HEADER_VALUE)

        assertAssertionPassed(result)
    }


    @Test
    void matchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_NAME, HEADER_VALUE)

        assertTrue("Expected to find header value", result.passed)
        assertFalse("Expected to find header value", result.failed)
    }

    @Test
    void matchingNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_NAME, NOT_MATCHING_HEADER_VALUE)

        assertFalse("Expected not to find header value", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }

    @Test
    void notMatchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, NOT_MATCHING_HEADER_NAME, HEADER_VALUE)

        assertFalse("Expected not to find header value", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }

    @Test
    void notMatchingNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, NOT_MATCHING_HEADER_NAME, NOT_MATCHING_HEADER_VALUE)

        assertFalse("Expected not to find header value", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }
}
