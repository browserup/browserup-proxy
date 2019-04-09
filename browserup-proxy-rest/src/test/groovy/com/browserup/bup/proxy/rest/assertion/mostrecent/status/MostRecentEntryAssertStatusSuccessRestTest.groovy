package com.browserup.bup.proxy.rest.assertion.mostrecent.status

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.rest.BaseRestTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.hamcrest.Matchers
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static org.junit.Assert.*

class MostRecentEntryAssertStatusSuccessRestTest extends BaseRestTest {
    def urlOfMostRecentRequest = 'url-most-recent'
    def urlOfOldRequest = 'url-old'
    def urlPatternToMatchUrl = '.*url-.*'
    def urlPatternNotToMatchUrl = '.*does_not_match-.*'
    def successStatus = HttpStatus.SC_OK
    def nonSuccessStatus = HttpStatus.SC_BAD_REQUEST
    def responseBody = "success"

    @Override
    String getUrlPath() {
        return 'har/mostRecentEntry/assertStatusSuccess'
    }

    @Test
    void getBadRequestUrlPatternIsInvalid() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [urlPattern: '[']
            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
    }

    @Test
    void statusSuccessPasses() {
        sendRequestsToTargetServer(nonSuccessStatus, successStatus)

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
                
                assertFalse('Expected assertion entry result to have "false" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void statusSuccessFails() {
        sendRequestsToTargetServer(successStatus, nonSuccessStatus)

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern]
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
        sendRequestsToTargetServer(successStatus, nonSuccessStatus)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPatternNotToMatchUrl]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get no assertion result entries', assertionResult.requests, Matchers.hasSize(0))
                assertAssertionPassed(assertionResult)
                
            }
        }
    }

    private void sendRequestsToTargetServer(int oldStatus, int recentStatus) {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, recentStatus)
        mockTargetServerResponse(urlOfOldRequest, responseBody, oldStatus)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlOfOldRequest, responseBody)

        sleep MILLISECONDS_BETWEEN_REQUESTS

        requestToTargetServer(urlOfMostRecentRequest, responseBody)
    }

    protected void mockTargetServerResponse(String url, String responseBody, int status) {
        stubFor(get(urlEqualTo("/${url}")).
                willReturn(
                        ok().
                                withStatus(status).
                                withHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, 'text/plain').
                                withBody(responseBody))
        )
    }
}
