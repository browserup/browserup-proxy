package com.browserup.bup.proxy.rest.assertion.entries.header

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.rest.BaseRestTest
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

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

    protected static final HttpHeader FIRST_HEADER = new HttpHeader(
            FIRST_HEADER_NAME,
            FIRST_HEADER_VALUE
    )
    protected static final HttpHeader SECOND_HEADER = new HttpHeader(
            SECOND_HEADER_NAME,
            SECOND_HEADER_VALUE
    )

    protected void mockTargetServerResponse(String url, String responseBody, HttpHeader... headers) {
        stubFor(get(urlEqualTo("/${url}")).
                willReturn(
                        ok().
                                withHeaders(
                                        new HttpHeaders(headers).
                                                plus(new HttpHeader('Content-Type', 'text/plain'))
                                ).
                                withBody(responseBody))
        )
    }

    protected void sendRequestsToTargetServer(HttpHeader firstResponseHeader, HttpHeader secondResponseHeader) {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_HEADERS)

        mockTargetServerResponse(URL_OF_FIRST_REQUEST, COMMON_RESPONSE_BODY, firstResponseHeader)
        mockTargetServerResponse(URL_OF_SECOND_REQUEST, COMMON_RESPONSE_BODY, secondResponseHeader)

        proxyManager.get()[0].newHar()

        requestToTargetServer(URL_OF_FIRST_REQUEST, COMMON_RESPONSE_BODY)
        requestToTargetServer(URL_OF_SECOND_REQUEST, COMMON_RESPONSE_BODY)
    }
}
