package com.browserup.bup.proxy.rest.assertion.entries.header

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.CaptureType
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpResponseDecorator
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.eclipse.jetty.http.HttpMethods
import org.hamcrest.Matchers
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class EntriesAssertHeaderMatchesRestTest extends BaseEntriesAssertHeaderRestTest {
    protected static final String HEADER_NAME_PATTERN_TO_MATCH_FIRST = ".*${FIRST_HEADER_NAME}.*"
    protected static final String HEADER_NAME_PATTERN_TO_MATCH_BOTH = ".*${COMMON_HEADER_NAME}.*"
    protected static final String HEADER_VALUE_PATTERN_TO_MATCH_FIRST = ".*${FIRST_HEADER_VALUE}.*"
    protected static final String HEADER_VALUE_PATTERN_TO_MATCH_SECOND = ".*${SECOND_HEADER_VALUE}.*"
    protected static final String HEADER_VALUE_PATTERN_TO_MATCH_ALL = ".*"

    @Override
    String getUrlPath() {
        return 'har/entries/assertResponseHeaderMatches'
    }

    @Test
    void urlFilterMatchesBothAndAnyHeaderNameAndHeaderValueMatchesPasses() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerValuePattern: HEADER_VALUE_PATTERN_TO_MATCH_ALL
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesBothAndAnyHeaderNameAndHeaderValueDoesNotMatchesFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerValuePattern: HEADER_VALUE_PATTERN_TO_MATCH_FIRST
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionFailed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesBothAndHeaderNameMatchesFirstAndHeaderValueMatchesFirstPasses() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerNamePattern: HEADER_NAME_PATTERN_TO_MATCH_FIRST,
                    headerValuePattern: HEADER_VALUE_PATTERN_TO_MATCH_FIRST
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesBothAndHeaderNameMatchesFirstAndHeaderValueMatchesSecondFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerNamePattern: HEADER_NAME_PATTERN_TO_MATCH_FIRST,
                    headerValuePattern: HEADER_VALUE_PATTERN_TO_MATCH_SECOND
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionFailed(assertionResult)

                def failedRequests = assertionResult.failedRequests

                assertThat('Expected to get one failed assertion entry', failedRequests, Matchers.hasSize(1))
                assertThat('Expected assertion entry to have proper url',
                        failedRequests.get(0).url,
                        Matchers.containsString(URL_OF_FIRST_REQUEST))
            }
        }
    }

    @Test
    void urlFilterMatchesBothAndHeaderNameMatchesBothAndHeaderValueMatchesSecondFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerNamePattern: HEADER_NAME_PATTERN_TO_MATCH_BOTH,
                    headerValuePattern: HEADER_VALUE_PATTERN_TO_MATCH_SECOND
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionFailed(assertionResult)

                def failedRequests = assertionResult.failedRequests

                assertThat('Expected to get one failed assertion entry', failedRequests, Matchers.hasSize(1))
                assertThat('Expected assertion entry to have proper url',
                        failedRequests.get(0).url,
                        Matchers.containsString(URL_OF_FIRST_REQUEST))
            }
        }
    }

    @Test
    void urlFilterMatchesNonePasses() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    headerNamePattern: HEADER_NAME_PATTERN_TO_MATCH_BOTH,
                    headerValuePattern: HEADER_VALUE_PATTERN_TO_MATCH_SECOND
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertAssertionPassed(assertionResult)
            }
        }
    }


    @Test
    void getBadRequestIfHeaderValuePatternNotProvided() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    headerNamePattern: HEADER_NAME_PATTERN_TO_MATCH_BOTH
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
    void getBadRequestIfHeaderValuePatternNotValid() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    headerNamePattern: HEADER_NAME_PATTERN_TO_MATCH_BOTH,
                    headerValuePattern: '['
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
    void getBadRequestIfHeaderNamePatternNotValid() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    headerNamePattern: '[',
                    headerValuePattern: HEADER_VALUE_PATTERN_TO_MATCH_SECOND
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
