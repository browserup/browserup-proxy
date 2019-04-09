package com.browserup.bup.proxy.rest.assertion.mostrecent

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.rest.BaseRestTest
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.*

class MostRecentEntryAssertTimeLessThanOrEqualRestTest extends BaseRestTest {
    def successfulAssertionMilliseconds = SUCCESSFUL_ASSERTION_TIME_WITHIN
    def failedAssertionMilliseconds = FAILED_ASSERTION_TIME_WITHIN
    def urlOfMostRecentRequest = 'url-most-recent'
    def urlOfOldRequest = 'url-old'
    def commonUrlPattern = '.*url-.*'
    def responseBody = 'success'
    def urlPattern = '.*does-not-match.*'
    def assertionMilliseconds = SUCCESSFUL_ASSERTION_TIME_WITHIN

    @Override
    String getUrlPath() {
        return 'har/mostRecentEntry/assertResponseTimeLessThanOrEqual'
    }

    @Test
    void passAndFailTimeWithinAssertion() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            def urlPattern = ".*${commonUrlPattern}"
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern, milliseconds: successfulAssertionMilliseconds]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
                
                assertFalse('Expected assertion entry result to have "false" failed flag', assertionResult.requests[0].failed)
            }
        }

        sendGetToProxyServer { req ->
            def urlPattern = ".*${commonUrlPattern}"
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern, milliseconds: failedAssertionMilliseconds]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionFailed(assertionResult)
                
                assertTrue('Expected assertion entry result to have "true" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void getEmptyResultIfNoEntryFoundByUrlPattern() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern, milliseconds: assertionMilliseconds]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get no assertion result entries', assertionResult.requests, Matchers.hasSize(0))
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

    private void sendRequestsToTargetServer() {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, TARGET_SERVER_RESPONSE_DELAY)
        mockTargetServerResponse(urlOfOldRequest, responseBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlOfOldRequest, responseBody)

        sleep MILLISECONDS_BETWEEN_REQUESTS

        requestToTargetServer(urlOfMostRecentRequest, responseBody)
    }

    protected void mockTargetServerResponse(String url, String responseBody, Integer delayMilliseconds=0) {
        stubFor(get(urlEqualTo("/${url}")).
                willReturn(
                        ok().
                                withFixedDelay(delayMilliseconds).
                                withHeader(HttpHeaders.CONTENT_TYPE, 'text/plain').
                                withBody(responseBody))
        )
    }
}
