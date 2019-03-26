package com.browserup.bup.proxy.assertion.field.content

import org.apache.commons.lang3.StringUtils
import org.apache.http.client.methods.HttpGet
import org.junit.Test

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*

class ContentSizeUnderTest extends ContentBaseTest {

    @Test
    void contentSizeWithinAssertionPasses() {
        def body = StringUtils.repeat('a', 100)
        def bodySize = body.bytes.size()

        mockResponse(URL_PATH, body)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", body, respBody)

        def result = proxy.assertUrlContentLengthUnder(Pattern.compile(".*${URL_PATH}.*"), bodySize)

        assertTrue("Expected assertion to pass", result.passed)
        assertFalse("Expected assertion to pass", result.failed)
    }

    @Test
    void contentSizeWithinAssertionFails() {
        def body = StringUtils.repeat('a', 100)
        def bodySize = body.bytes.size()

        mockResponse(URL_PATH, body)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", body, respBody)

        def result = proxy.assertUrlContentLengthUnder(Pattern.compile(".*${URL_PATH}.*"), bodySize - 1)

        assertFalse("Expected assertion to fail", result.passed)
        assertTrue("Expected assertion to fail", result.failed)
    }
}
