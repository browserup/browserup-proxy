package com.browserup.bup.proxy.rest.assertion.entries

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.rest.BaseRestTest
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpResponseDecorator
import org.apache.http.HttpStatus
import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.*

class EntriesAssertTimeLessThanOrEqualRestTest extends BaseRestTest {
    def responseBody = 'success'
    def url = 'some-url'
    def urlPatternToMatchUrl = '.*url-.*'
    def urlPatternNotToMatchUrl = '.*does_not_match-.*'

    @Override
    String getUrlPath() {
        return 'har/entries/assertResponseTimeLessThanOrEqual'
    }

    @Test
    void someEntriesFailTimeWithinAssertion() {
        def fastRange = (1..2)
        def slowRange = (3..4)

        def urlPathCreator = { index -> "${url}-${index}" }

        def fastUrls = fastRange.collect(urlPathCreator)
        def slowUrls = slowRange.collect(urlPathCreator)
        def allUrls = fastUrls + slowUrls

        fastUrls.forEach { mockTargetServerResponse(it, responseBody) }
        slowUrls.forEach { mockTargetServerResponse(it, responseBody, TARGET_SERVER_SLOW_RESPONSE_DELAY) }

        proxyManager.get()[0].newHar()

        allUrls.forEach { requestToTargetServer(it, responseBody) }

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPatternToMatchUrl, milliseconds: SUCCESSFUL_ASSERTION_TIME_WITHIN]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get all assertion entries filtered by url pattern',
                        assertionResult.requests, Matchers.hasSize(allUrls.size()))
                assertAssertionFailed(assertionResult)
                

                assertionResult.requests.forEach { e ->
                    if (fastUrls.find {e.url.contains(it)}) {
                        assertFalse("Expected entry result for fast response to have failed flag = false", e.failed)
                    }
                    if (slowUrls.find {e.url.contains(it)}) {
                        assertTrue("Expected entry result for slow response to have failed flag = true", e.failed)
                    }
                }
            }
        }
    }

    @Test
    void emptyResultIfNoEntriesFoundForTimeWithinAssertion() {
        proxyManager.get()[0].newHar()

        mockTargetServerResponse(url, responseBody)

        requestToTargetServer(url, responseBody)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPatternNotToMatchUrl, milliseconds: SUCCESSFUL_ASSERTION_TIME_WITHIN]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get empty assertion entries found by url pattern',
                        assertionResult.requests, Matchers.hasSize(0))
                assertAssertionPassed(assertionResult)
                
            }
        }
    }

    @Test
    void getBadRequestIfMillisecondsNotValid() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.query = [urlPattern: '.*', milliseconds: 'abcd']
            uri.path = fullUrlPath
            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
    }
}