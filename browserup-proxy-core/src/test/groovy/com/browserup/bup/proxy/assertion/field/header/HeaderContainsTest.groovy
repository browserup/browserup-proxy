package com.browserup.bup.proxy.assertion.field.header


import org.apache.http.client.methods.HttpGet
import org.junit.Test

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*

class HeaderContainsTest extends HeaderBaseTest {

    @Test
    void headerContainsTextAssertionPasses() {
        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderContains(Pattern.compile(".*${URL_PATH}.*"), HEADER_VALUE)

        assertTrue("Expected to find header value", result.passed)
        assertFalse("Expected to find header value", result.failed)

        result = proxy.assertUrlResponseHeaderContains(Pattern.compile(".*${URL_PATH}.*"), HEADER_NAME)

        assertTrue("Expected to find header name", result.passed)
        assertFalse("Expected to find header name", result.failed)
    }

    @Test
    void headerContainsTextAssertionFails() {
        def headerNotToFind = 'abc'

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlResponseHeaderContains(Pattern.compile(".*${URL_PATH}.*"), headerNotToFind)

        assertFalse("Expected not to find header value", result.passed)
        assertTrue("Expected not to find header value", result.failed)

        result = proxy.assertUrlResponseHeaderContains(Pattern.compile(".*${URL_PATH}.*"), headerNotToFind)

        assertFalse("Expected not to find header name", result.passed)
        assertTrue("Expected not to find header name", result.failed)
    }
}
