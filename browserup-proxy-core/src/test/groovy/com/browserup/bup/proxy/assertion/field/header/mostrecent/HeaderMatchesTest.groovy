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

        assertTrue("Expected all headers to match value pattern", result.passed)
        assertFalse("Expected all headers to match value pattern", result.failed)
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

        assertTrue("Expected all headers to match value pattern", result.passed)
        assertFalse("Expected all headers to match value pattern", result.failed)
    }

    @Test
    void matchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${HEADER_NAME}.*"),
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertTrue("Expected headers values found by name pattern to match value pattern", result.passed)
        assertFalse("Expected headers values found by name pattern to match value pattern", result.failed)
    }

    @Test
    void matchingNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${HEADER_NAME}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertFalse("Expected headers values found by name pattern not to match value pattern", result.passed)
        assertTrue("Expected headers values found by name pattern not to match value pattern", result.failed)
    }

    @Test
    void notMatchingNameAndMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_NAME}.*"),
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertTrue("Expected to pass when no header found by name pattern", result.passed)
        assertFalse("Expected to pass when no header found by name pattern", result.failed)
    }

    @Test
    void notMatchingNameAndNotMatchingValue() {
        requestToMockedServer(URL_PATH)

        def result = proxy.assertMostRecentResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_NAME}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertTrue("Expected to pass when no header found by name pattern", result.passed)
        assertFalse("Expected to pass when no header found by name pattern", result.failed)
    }
}
