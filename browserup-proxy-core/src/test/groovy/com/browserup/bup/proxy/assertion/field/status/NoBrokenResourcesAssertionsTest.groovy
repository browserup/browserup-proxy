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

class NoBrokenResourcesAssertionsTest extends BaseAssertionsTest {
    @Test
    void noBrokenJavaScriptLinksWhenContainsCorrectTypeAndStatusCode2xxPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", "text/javascript"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenJavaScriptLinks()

        assertTrue("Expected to find not broken links", result.passed)
        assertFalse("Expected to find not broken links", result.failed)
    }

    @Test
    void noBrokenJavaScriptLinksWhenContainsCorrectTypeAndStatusCode3xxPasses() {
        def status = HttpStatus.SC_MOVED_TEMPORARILY

        mockResponse(Header.header("Content-Type", "text/javascript"), status)
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
    void noBrokenJavaScriptLinksWhenContainsCorrectTypeAndStatus4xxFails() {
        def status = HttpStatus.SC_BAD_REQUEST

        mockResponse(Header.header("Content-Type", "text/javascript"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenJavaScriptLinks()

        assertTrue("Expected to find broken links", result.failed)
        assertFalse("Expected to find broken links", result.passed)
    }

    @Test
    void noBrokenImageLinksWhenContainsCorrectTypeAndStatusPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", "image/jpg"), status)
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
    void noBrokenImageLinksWhenContainsCorrectTypeAndStatus4xxFails() {
        def status = HttpStatus.SC_NOT_FOUND

        mockResponse(Header.header("Content-Type", "image/jpg"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenImages()

        assertTrue("Expected to find broken links", result.failed)
        assertFalse("Expected to find broken links", result.passed)
    }

    @Test
    void noBrokenStyleSheetsWhenContainsCorrectTypeAndStatusCode2xxPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", "text/css"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenStyleSheets()

        assertTrue("Expected to find not broken links", result.passed)
        assertFalse("Expected to find not broken links", result.failed)
    }

    @Test
    void noBrokenStyleSheetsWhenContainsCorrectTypeAndStatusCode3xxPasses() {
        def status = HttpStatus.SC_MOVED_TEMPORARILY

        mockResponse(Header.header("Content-Type", "text/css"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenStyleSheets()

        assertTrue("Expected to find not broken links", result.passed)
        assertFalse("Expected to find not broken links", result.failed)
    }

    @Test
    void noBrokenStyleSheetsWhenNoJavaScriptHeadersPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", ""), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenStyleSheets()

        assertTrue("Expected to find not broken links", result.passed)
        assertFalse("Expected to find not broken links", result.failed)
    }

    @Test
    void noBrokenStyleSheetsWhenContainsCorrectTypeAndStatus4xxFails() {
        def status = HttpStatus.SC_BAD_REQUEST

        mockResponse(Header.header("Content-Type", "text/css"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenStyleSheets()

        assertTrue("Expected to find broken links", result.failed)
        assertFalse("Expected to find broken links", result.passed)
    }

    @Test
    void noBrokenMediaTypeWhenContainsCorrectTypeAndStatusCode2xxPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(Header.header("Content-Type", "text/csv"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenMediaType("text/csv")

        assertTrue("Expected to find not broken links", result.passed)
        assertFalse("Expected to find not broken links", result.failed)
    }

    @Test
    void noBrokenMediaTypeWhenContainsCorrectTypeAndStatus4xxFails() {
        def status = HttpStatus.SC_BAD_REQUEST

        mockResponse(Header.header("Content-Type", "text/csv"), status)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertNoBrokenMediaType("text/csv")

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
