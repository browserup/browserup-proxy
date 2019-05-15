package com.browserup.bup.proxy.assertion.field.status

import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import org.apache.http.HttpStatus
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class BrokenResourcesAssertionsTest extends BaseAssertionsTest {
    @Test
    void noBrokenJavaScriptLinksWhenContainsCorrectTypeAndStatusPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", "text/javascript", "javascript"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenJavaScriptLinks()

        assertTrue("Expected to find not broken links", result.passed)
        assertFalse("Expected to find not broken links", result.failed)
    }

    @Test
    void noBrokenJavaScriptLinksWhenNoJavaScriptHeadersPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", ""), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenJavaScriptLinks()

        assertTrue("Expected to find not broken links", result.passed)
        assertFalse("Expected to find not broken links", result.failed)
    }

    @Test
    void noBrokenJavaScriptLinksWhenContainsCorrectTypeAndStatusNotFoundFails() {
        def status = HttpStatus.SC_NOT_FOUND

        mockResponse(Header.header("Content-Type", "text/javascript", "javascript"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenJavaScriptLinks()

        assertTrue("Expected to find broken links", result.failed)
        assertFalse("Expected to find broken links", result.passed)
    }

    @Test
    void noBrokenImageLinksWhenContainsCorrectTypeAndStatusPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", "text/image", "image"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenImages()

        assertTrue("Expected to find not broken links", result.passed)
        assertFalse("Expected to find not broken links", result.failed)
    }

    @Test
    void noBrokenJavaScriptLinksWhenNoImageHeadersPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", ""), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenImages()

        assertTrue("Expected to pass when header not found", result.passed)
        assertFalse("Expected to pass when header not found", result.failed)
    }

    @Test
    void noBrokenImageLinksWhenContainsCorrectTypeAndStatusNotFoundFails() {
        def status = HttpStatus.SC_NOT_FOUND

        mockResponse(Header.header("Content-Type", "text/image", "image"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenImages()

        assertTrue("Expected to find broken links", result.failed)
        assertFalse("Expected to find broken links", result.passed)
    }

    protected mockResponse(Header header, Integer status) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/" + URL_PATH),
                Times.once())
                .respond(response()
                        .withStatusCode(status)
                        .withHeader(header)
                        .withBody(SUCCESSFUL_RESPONSE_BODY))
    }
}
