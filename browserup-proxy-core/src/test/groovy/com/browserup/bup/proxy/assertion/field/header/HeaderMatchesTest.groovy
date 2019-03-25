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

        assertTrue("Expected to find header value", result.passed)
        assertFalse("Expected to find header value", result.failed)
    }

    @Test
    void anyNameAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertFalse("Expected not to find header value", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }

    @Test
    void anyNameIfEmptyNameProvidedAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                null,
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertTrue("Expected to find header value", result.passed)
        assertFalse("Expected to find header value", result.failed)
    }


    @Test
    void matchingNameAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${HEADER_NAME}.*"),
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertTrue("Expected to find header value", result.passed)
        assertFalse("Expected to find header value", result.failed)
    }

    @Test
    void matchingNameAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)


        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${HEADER_NAME}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertFalse("Expected not to find header value", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }

    @Test
    void notMatchingNameAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_NAME}.*"),
                Pattern.compile(".*${HEADER_VALUE}.*"))

        assertFalse("Expected not to find header value", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }

    @Test
    void notMatchingNameAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderMatches(
                Pattern.compile(".*${URL_PATH}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_NAME}.*"),
                Pattern.compile(".*${NOT_MATCHING_HEADER_VALUE}.*"))

        assertFalse("Expected not to find header value", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }
}
