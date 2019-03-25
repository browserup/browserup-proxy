package com.browserup.bup.proxy.assertion.field.header


import org.apache.http.client.methods.HttpGet
import org.junit.Test

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*

class HeaderDoesNotContainTest extends HeaderBaseTest {

    @Test
    void anyNameAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), NOT_MATCHING_HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)
    }

    @Test
    void nameNotProvidedAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), null, NOT_MATCHING_HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)

        result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), '', NOT_MATCHING_HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)
    }

    @Test
    void nameNotProvidedAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), null, HEADER_VALUE)

        assertFalse("Expected to find header value", result.passed)
        assertTrue("Expected to find header value", result.failed)

        result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), '', HEADER_VALUE)

        assertFalse("Expected to find header value", result.passed)
        assertTrue("Expected to find header value", result.failed)
    }

    @Test
    void anyNameAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), HEADER_VALUE)

        assertFalse("Expected to find header value", result.passed)
        assertTrue("Expected to find header value", result.failed)
    }

    @Test
    void matchingNameAndNotMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"),
                HEADER_NAME, NOT_MATCHING_HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)
    }

    @Test
    void notMatchingNameAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"),
                NOT_MATCHING_HEADER_NAME, HEADER_VALUE)

        assertTrue("Expected not to find header value", result.passed)
        assertFalse("Expected not to find header value", result.failed)
    }

    @Test
    void matchingNameAndMatchingValue() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderDoesNotContain(Pattern.compile(".*${URL_PATH}.*"),
                HEADER_NAME, HEADER_VALUE)

        assertFalse("Expected to find header value", result.passed)
        assertTrue("Expected to find header value", result.failed)
    }
}
