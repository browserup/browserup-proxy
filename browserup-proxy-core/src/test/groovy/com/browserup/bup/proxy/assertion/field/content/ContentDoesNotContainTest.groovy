package com.browserup.bup.proxy.assertion.field.content

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import org.apache.http.client.methods.HttpGet
import org.junit.Before
import org.junit.Test
import org.mockserver.model.Delay

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*

class ContentDoesNotContainTest extends ContentBaseTest {

    @Test
    void contentDoesNotContainTextAssertionPasses() {
        def bodyPart = 'body part'
        def body = 'body example'

        mockResponse(URL_PATH, body)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", body, respBody)

        def result = proxy.assertUrlContentDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), bodyPart)

        assertTrue("Expected not to find text in content", result.passed)
        assertFalse("Expected not to find text in content", result.failed)
    }

    @Test
    void contentDoesNotContainTextAssertionFails() {
        def bodyPart = 'body part'
        def body = "body example with ${bodyPart} in the middle".toString()

        mockResponse(URL_PATH, body)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", body, respBody)

        def result = proxy.assertUrlContentDoesNotContain(Pattern.compile(".*${URL_PATH}.*"), bodyPart)

        assertFalse("Expected to find text in content", result.passed)
        assertTrue("Expected to find text in content", result.failed)
    }
}
