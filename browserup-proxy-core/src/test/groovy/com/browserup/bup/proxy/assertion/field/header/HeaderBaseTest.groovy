package com.browserup.bup.proxy.assertion.field.header

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import org.apache.http.HttpStatus
import org.junit.Before
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class HeaderBaseTest extends BaseAssertionsTest {
    protected static final def HEADER_NAME = 'headerName'
    protected static final def NOT_MATCHING_HEADER_NAME = 'headerName not to match'
    protected static final def HEADER_VALUE = 'headerValue'
    protected static final def NOT_MATCHING_HEADER_VALUE = 'headerValue not to match'
    protected static final def HEADER = Header.header(HEADER_NAME, HEADER_VALUE)

    @Before
    void setUp() {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_HEADERS)
        mockResponse(URL_PATH, HEADER)
    }

    protected mockResponse(String path, Header header) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${path}"),
                Times.once())
                .respond(response()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(SUCCESSFUL_RESPONSE_BODY)
                .withHeader(header))
    }
}
