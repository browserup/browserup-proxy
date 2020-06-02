package com.browserup.bup.proxy.mitmproxy.assertion.entries.content

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.mitmproxy.BaseRestTest
import org.apache.http.HttpStatus
import org.junit.Test

import static org.junit.Assert.assertEquals

abstract class BaseEntriesAssertContentRestTest extends BaseRestTest {
    protected static final String COMMON_URL_PART = 'url'
    protected static final String URL_OF_FIRST_REQUEST = "${COMMON_URL_PART}-first"
    protected static final String URL_OF_SECOND_REQUEST = "${COMMON_URL_PART}-second"
    protected static final String URL_PATTERN_TO_MATCH_BOTH = ".*${COMMON_URL_PART}-.*"
    protected static final String URL_PATTERN_TO_MATCH_FIRST = ".*${URL_OF_FIRST_REQUEST}.*"
    protected static final String URL_PATTERN_TO_MATCH_NOTHING = '.*does_not_match-.*'
    protected static final String RESPONSE_COMMON_PART = 'some-body-part'
    protected static final String FIRST_RESPONSE = "first respone ${RESPONSE_COMMON_PART} end first"
    protected static final String SECOND_RESPONSE = "second respone ${RESPONSE_COMMON_PART} end second"
    protected static final String RESPONSE_NOT_TO_FIND = 'nothing'

    protected void sendRequestsToTargetServer(String firstBody, String secondBody) {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_CONTENT)

        mockTargetServerResponse(URL_OF_FIRST_REQUEST, firstBody)
        mockTargetServerResponse(URL_OF_SECOND_REQUEST, secondBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(URL_OF_FIRST_REQUEST, firstBody)
        requestToTargetServer(URL_OF_SECOND_REQUEST, secondBody)
    }

    @Test
    void getBadRequestIfUrlPatternNotProvided() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    contentText: RESPONSE_NOT_TO_FIND
            ]
            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
            response.success = { resp, reader ->
                throw new AssertionError('Expected to get bad request, got: ' + resp.status)
            }
        }
    }

    @Test
    void getBadRequestIfUrlPatternNotValid() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: '[',
                    contentText: RESPONSE_NOT_TO_FIND
            ]
            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
            response.success = { resp, reader ->
                throw new AssertionError('Expected to get bad request, got: ' + resp.status)
            }
        }
    }
}
