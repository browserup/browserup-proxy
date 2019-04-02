package com.browserup.bup.proxy.assertion.field.content

import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ContentMatchesTest extends ContentBaseTest {

    @Test
    void contentMatchesAssertionPasses() {
        def pattern = '.*something.*'
        def body = "body example with something in the middle".toString()

        mockResponse(URL_PATH, body)

        requestToMockedServer(URL_PATH, body)

        def result = proxy.assertMostRecentResponseContentMatches(Pattern.compile(".*${URL_PATH}.*"), Pattern.compile(pattern))

        assertTrue("Expected assertion to pass", result.passed)
        assertFalse("Expected assertion to pass", result.failed)
    }

    @Test
    void contentMatchesAssertionFails() {
        def pattern = '.*something.*'
        def body = 'body example'

        mockResponse(URL_PATH, body)

        requestToMockedServer(URL_PATH, body)

        def result = proxy.assertMostRecentResponseContentMatches(Pattern.compile(".*${URL_PATH}.*"), Pattern.compile(pattern))

        assertFalse("Expected assertion to fail", result.passed)
        assertTrue("Expected assertion to fail", result.failed)
    }
}
