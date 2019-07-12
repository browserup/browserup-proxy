package com.browserup.bup.proxy.assertion.field.header

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.apache.http.HttpStatus
import org.junit.Before

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class HeaderBaseTest extends BaseAssertionsTest {
    protected static final def HEADER_NAME = 'headerName'
    protected static final def NOT_MATCHING_HEADER_NAME = 'headerName not to match'
    protected static final def HEADER_VALUE = 'headerValue'
    protected static final def NOT_MATCHING_HEADER_VALUE = 'headerValue not to match'
    protected static final def HEADER = new HttpHeader(HEADER_NAME, HEADER_VALUE)

    @Before
    void setUp() {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_HEADERS)
        mockResponse(URL_PATH, HEADER)
    }

    protected mockResponse(String path, HttpHeader header) {
        stubFor(get(urlEqualTo("/" + path))
                .willReturn(ok()
                .withBody(SUCCESSFUL_RESPONSE_BODY)
                .withHeaders(new HttpHeaders(header))
        ))
//        mockServer.when(request()
//                .withMethod("GET")
//                .withPath("/${path}"),
//                Times.once())
//                .respond(response()
//                .withStatusCode(HttpStatus.SC_OK)
//                .withBody(SUCCESSFUL_RESPONSE_BODY)
//                .withHeader(header))
    }
}
