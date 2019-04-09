package com.browserup.bup.proxy.rest.assertion.entries.header

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.rest.BaseRestTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.eclipse.jetty.http.HttpMethod
import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.*

class EntriesAssertHeaderContainsRestTest extends BaseEntriesAssertHeaderRestTest {

    @Override
    String getUrlPath() {
        return 'har/entries/assertResponseHeaderContains'
    }

    @Test
    void urlFilterMatchesBothAndHeaderValueContainsInBothPasses() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerValue: COMMON_HEADER_VALUE
            ]

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesBothAndAnyHeaderNameAndHeaderValueMissedFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerValue: MISSING_HEADER_VALUE
            ]

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionFailed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndAnyHeaderNameAndHeaderValueMissedFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerValue: SECOND_HEADER_VALUE
            ]

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionFailed(assertionResult)

                def failedRequests = assertionResult.failedRequests

                assertThat('Expected to get one assertion entry', failedRequests, Matchers.hasSize(1))
                assertThat('Expected assertion entry to have proper url',
                        failedRequests.get(0).url,
                        Matchers.containsString(URL_OF_FIRST_REQUEST))
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndAnyHeaderNameAndHeaderValueContainsPassed() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerValue: FIRST_HEADER_VALUE
            ]

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndFirstHeaderNameAndFirstHeaderValuePassed() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerName: FIRST_HEADER_NAME,
                    headerValue: FIRST_HEADER_VALUE
            ]

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndSecondHeaderNameAndSecondHeaderValueContainsFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerName: SECOND_HEADER_NAME,
                    headerValue: SECOND_HEADER_VALUE
            ]

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionFailed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndFirstHeaderNameAndSecondHeaderValueContainsFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerName: FIRST_HEADER_NAME,
                    headerValue: SECOND_HEADER_VALUE
            ]

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionFailed(assertionResult)
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
                    headerName: FIRST_HEADER_NAME,
                    headerValue: SECOND_HEADER_VALUE
            ]

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void getBadRequestIfHeaderValueNotProvided() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    headerName: FIRST_HEADER_NAME
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
