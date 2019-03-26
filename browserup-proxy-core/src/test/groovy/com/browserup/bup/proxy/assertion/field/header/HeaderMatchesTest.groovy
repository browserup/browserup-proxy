package com.browserup.bup.proxy.assertion.field.header

import org.apache.http.client.methods.HttpGet
import org.junit.Test

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*

class HeaderMatchesTest extends HeaderBaseTest {

    @Test
    void anyNameAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*"))

        assertTrue("Expected all headers to match value pattern", result.passed)
        assertFalse("Expected all headers to match value pattern", result.failed)
    }

    @Test
    void anyNameAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertFalse("Expected headers not to match value pattern", result.passed)
        assertTrue("Expected headers not to match value pattern", result.failed)
    }

    @Test
    void emptyNameProvidedAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                null,
                Pattern.compile(".*"))

        assertTrue("Expected all headers to match value pattern", result.passed)
        assertFalse("Expected all headers to match value pattern", result.failed)
    }

    @Test
    void matchingNameAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${HEADER_NAME}.*"),
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertTrue("Expected headers values found by name pattern to match value pattern", result.passed)
        assertFalse("Expected headers values found by name pattern to match value pattern", result.failed)
    }

    @Test
    void matchingNameAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)


        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${HEADER_NAME}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertFalse("Expected headers values found by name pattern not to match value pattern", result.passed)
        assertTrue("Expected headers values found by name pattern not to match value pattern", result.failed)
    }

    @Test
    void notMatchingNameAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_NAME}.*"),
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertTrue("Expected to pass when no header found by name pattern", result.passed)
        assertFalse("Expected to pass when no header found by name pattern", result.failed)
    }

    @Test
    void notMatchingNameAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_NAME}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertTrue("Expected to pass when no header found by name pattern", result.passed)
        assertFalse("Expected to pass when no header found by name pattern", result.failed)
    }
}
