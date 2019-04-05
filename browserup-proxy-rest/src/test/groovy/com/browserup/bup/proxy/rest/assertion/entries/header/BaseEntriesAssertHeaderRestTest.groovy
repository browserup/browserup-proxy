package com.browserup.bup.proxy.rest.assertion.entries.header

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.rest.BaseRestTest
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.eclipse.jetty.http.HttpMethods
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

abstract class BaseEntriesAssertHeaderRestTest extends BaseRestTest {
    protected static final String COMMON_URL_PART = 'url'
    protected static final String URL_OF_FIRST_REQUEST = "${COMMON_URL_PART}-first"
    protected static final String URL_OF_SECOND_REQUEST = "${COMMON_URL_PART}-second"
    protected static final String URL_PATTERN_TO_MATCH_BOTH = ".*${COMMON_URL_PART}-.*"
    protected static final String URL_PATTERN_TO_MATCH_FIRST = ".*${URL_OF_FIRST_REQUEST}.*"
    protected static final String URL_PATTERN_TO_MATCH_NOTHING = '.*does_not_match-.*'
    protected static final String COMMON_RESPONSE_BODY = 'success'
    protected static final String COMMON_HEADER_VALUE = 'header-value'
    protected static final String COMMON_HEADER_NAME = 'header-name'
    protected static final String FIRST_HEADER_NAME = "first-${COMMON_HEADER_NAME}"
    protected static final String SECOND_HEADER_NAME = "some-${COMMON_HEADER_NAME}"
    protected static final String FIRST_HEADER_VALUE = "first-value-${COMMON_HEADER_VALUE}" as String
    protected static final String SECOND_HEADER_VALUE = "second-value-${COMMON_HEADER_VALUE}" as String
    protected static final String MISSING_HEADER_VALUE = 'missing value'

    protected static final Header FIRST_HEADER = new Header(
            FIRST_HEADER_NAME,
            FIRST_HEADER_VALUE
    )
    protected static final Header SECOND_HEADER = new Header(
            SECOND_HEADER_NAME,
            SECOND_HEADER_VALUE
    )

    protected void mockTargetServerResponse(String url, String responseBody, Header... headers) {
        targetMockedServer.when(request()
                .withMethod(HttpMethods.GET)
                .withPath("/${url}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(responseBody)
                .withHeader(new Header(HttpHeaders.CONTENT_TYPE, 'text/plain'))
                .withHeaders(headers))
    }

    protected void sendRequestsToTargetServer(Header firstResponseHeader, Header secondResponseHeader) {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_HEADERS)

        mockTargetServerResponse(URL_OF_FIRST_REQUEST, COMMON_RESPONSE_BODY, firstResponseHeader)
        mockTargetServerResponse(URL_OF_SECOND_REQUEST, COMMON_RESPONSE_BODY, secondResponseHeader)

        proxyManager.get()[0].newHar()

        requestToTargetServer(URL_OF_FIRST_REQUEST, COMMON_RESPONSE_BODY)
        requestToTargetServer(URL_OF_SECOND_REQUEST, COMMON_RESPONSE_BODY)
    }
}
