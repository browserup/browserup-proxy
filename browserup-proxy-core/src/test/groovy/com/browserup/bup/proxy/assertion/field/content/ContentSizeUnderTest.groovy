package com.browserup.bup.proxy.assertion.field.content

import org.apache.commons.lang3.StringUtils
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ContentSizeUnderTest extends ContentBaseTest {

    @Test
    void contentSizeWithinAssertionPasses() {
        def body = StringUtils.repeat('a', 100)
        def bodySize = body.bytes.size()

        mockResponse(URL_PATH, body)

        requestToMockedServer(URL_PATH, body)

        def result = proxy.assertMostRecentResponseContentLengthUnder(Pattern.compile(".*${URL_PATH}.*"), bodySize)

        assertTrue("Expected assertion to pass", result.passed)
        assertFalse("Expected assertion to pass", result.failed)
    }

    @Test
    void contentSizeWithinAssertionFails() {
        def body = StringUtils.repeat('a', 100)
        def bodySize = body.bytes.size()

        mockResponse(URL_PATH, body)

        requestToMockedServer(URL_PATH, body)

        def result = proxy.assertMostRecentResponseContentLengthUnder(Pattern.compile(".*${URL_PATH}.*"), bodySize - 1)

        assertFalse("Expected assertion to fail", result.passed)
        assertTrue("Expected assertion to fail", result.failed)
    }
}
