package com.browserup.bup.proxy.assertion.field.header.mostrecent

import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.*

class HeaderMatchesTest extends HeaderBaseTest {

    @Test
    void anyNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*"))

        assertAssertionPassed(result)
    }

    @Test
    void anyNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertFalse("Expected headers not to match value pattern", result.passed)
        assertTrue("Expected headers not to match value pattern", result.failed)
    }

    @Test
    void emptyNameProvidedAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                null,
                Pattern.compile(".*"))

        assertAssertionPassed(result)
    }

    @Test
    void matchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${HEADER_NAME}.*"),
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertAssertionPassed(result)
    }

    @Test
    void matchingNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${HEADER_NAME}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertAssertionFailed(result)
    }

    @Test
    void notMatchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_NAME}.*"),
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertAssertionPassed(result)
    }

    @Test
    void notMatchingNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_NAME}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertAssertionPassed(result)
    }
}
