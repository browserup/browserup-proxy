package com.browserup.bup.proxy.assertion.field.content


import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ContentContainsTest extends ContentBaseTest {

    @Test
    void contentContainsTextAssertionPasses() {
        def bodyPart = 'body part'
        def body = "body example with ${bodyPart} in the middle".toString()

        mockResponse(URL_PATH, body)

        requestToMockedServer(URL_PATH, body)

        def result = proxy.assertMostRecentResponseContentContains(Pattern.compile(".*${URL_PATH}.*"), bodyPart)

        assertTrue("Expected assertion to pass", result.passed)
        assertFalse("Expected assertion to pass", result.failed)
    }

    @Test
    void contentContainsTextAssertionFails() {
        def bodyPart = 'body part'
        def body = 'body example'

        mockResponse(URL_PATH, body)

        requestToMockedServer(URL_PATH, body)

        def result = proxy.assertMostRecentResponseContentContains(Pattern.compile(".*${URL_PATH}.*"), bodyPart)

        assertFalse("Expected assertion to fail", result.passed)
        assertTrue("Expected assertion to fail", result.failed)
    }
}
