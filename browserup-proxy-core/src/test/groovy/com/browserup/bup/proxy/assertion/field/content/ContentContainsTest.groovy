package com.browserup.bup.proxy.assertion.field.content


import org.apache.http.client.methods.HttpGet
import org.junit.Test

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*

class ContentContainsTest extends ContentBaseTest {

    @Test
    void contentContainsTextAssertionPasses() {
        def bodyPart = 'body part'
        def body = "body example with ${bodyPart} in the middle".toString()

        mockResponse(URL_PATH, body)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", body, respBody)

        def result = proxy.assertUrlContentContains(Pattern.compile(".*${URL_PATH}.*"), bodyPart)

        assertTrue("Expected assertion to pass", result.passed)
        assertFalse("Expected assertion to pass", result.failed)
    }

    @Test
    void contentContainsTextAssertionFails() {
        def bodyPart = 'body part'
        def body = 'body example'

        mockResponse(URL_PATH, body)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", body, respBody)

        def result = proxy.assertUrlContentContains(Pattern.compile(".*${URL_PATH}.*"), bodyPart)

        assertFalse("Expected assertion to fail", result.passed)
        assertTrue("Expected assertion to fail", result.failed)
    }
}
