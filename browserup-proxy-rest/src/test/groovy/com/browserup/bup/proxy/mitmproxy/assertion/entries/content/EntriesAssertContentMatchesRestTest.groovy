package com.browserup.bup.proxy.mitmproxy.assertion.entries.content

import com.browserup.bup.assertion.model.AssertionResult
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpResponseDecorator
import org.apache.http.HttpStatus
import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

class EntriesAssertContentMatchesRestTest extends com.browserup.bup.proxy.mitmproxy.assertion.entries.content.BaseEntriesAssertContentRestTest {
    protected static final String CONTENT_PATTERN_TO_MATCH_BOTH = ".*${RESPONSE_COMMON_PART}.*"
    protected static final String CONTENT_PATTERN_TO_MATCH_FIRST = ".*${FIRST_RESPONSE}.*"
    protected static final String CONTENT_PATTERN_TO_MATCH_SECOND = ".*${SECOND_RESPONSE}.*"

    @Override
    String getUrlPath() {
        return 'har/entries/assertContentMatches'
    }

    @Test
    void urlFilterMatchesBothAndContentFilterMatchesBothPasses() {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    contentPattern: CONTENT_PATTERN_TO_MATCH_BOTH
            ]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesBothAndContentFilterDoesNotMatchSomeFails() {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    contentPattern: CONTENT_PATTERN_TO_MATCH_FIRST
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get two assertion entries', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionFailed(assertionResult)

                def failedRequests = assertionResult.failedRequests

                assertThat('Expected to get one failed assertion entry', failedRequests, Matchers.hasSize(1))
                assertThat('Expected failed entry to have proper request url',
                        failedRequests.get(0).url,
                        Matchers.containsString(URL_OF_SECOND_REQUEST))
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndContentFilterDoesNotMatchFirstFails() {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    contentPattern: CONTENT_PATTERN_TO_MATCH_SECOND
            ]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
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
    void urlFilterMatchesNonePasses() {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    contentPattern: CONTENT_PATTERN_TO_MATCH_SECOND
            ]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void getBadRequestIfContentPatternNotProvided() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING
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
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    contentPattern: '['
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
