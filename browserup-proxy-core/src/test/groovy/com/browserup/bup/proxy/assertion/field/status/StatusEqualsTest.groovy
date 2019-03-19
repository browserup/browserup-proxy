package com.browserup.bup.proxy.assertion.field.status

import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.junit.Test
import org.mockserver.matchers.Times

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class StatusEqualsTest extends BaseAssertionsTest {

    @Test
    void statusEqualsAssertionPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlStatusEquals(Pattern.compile(".*${URL_PATH}.*"), status)

        assertTrue("Expected to find status", result.passed)
        assertFalse("Expected to find header value", result.failed)
    }

    @Test
    void statusEqualsAssertionFails() {
        def expectedStatus = HttpStatus.SC_OK
        def status = HttpStatus.SC_BAD_REQUEST

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertUrlStatusEquals(Pattern.compile(".*${URL_PATH}.*"), expectedStatus)

        assertFalse("Expected not to find status", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }

    protected mockResponse(String path, Integer status) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${path}"),
                Times.once())
                .respond(response()
                .withStatusCode(status)
                .withBody(SUCCESSFUL_RESPONSE_BODY))
    }
}
