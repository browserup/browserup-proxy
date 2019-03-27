package com.browserup.bup.proxy.assertion.field.content

import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ContentDoesNotContainTest extends ContentBaseTest {

    @Test
    void contentDoesNotContainTextAssertionPasses() {
        def bodyPart = 'body part'
        def body = 'body example'

        mockResponse(URL_PATH, body)

        requestToMockedServer(URL_PATH, body)

        def result = proxy.assertMostRecentResponseContentDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), bodyPart)

        assertTrue("Expected not to find text in content", result.passed)
        assertFalse("Expected not to find text in content", result.failed)
    }

    @Test
    void contentDoesNotContainTextAssertionFails() {
        def bodyPart = 'body part'
        def body = "body example with ${bodyPart} in the middle".toString()

        mockResponse(URL_PATH, body)

        requestToMockedServer(URL_PATH, body)

        def result = proxy.assertMostRecentResponseContentDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), bodyPart)

        assertFalse("Expected to find text in content", result.passed)
        assertTrue("Expected to find text in content", result.failed)
    }
}
