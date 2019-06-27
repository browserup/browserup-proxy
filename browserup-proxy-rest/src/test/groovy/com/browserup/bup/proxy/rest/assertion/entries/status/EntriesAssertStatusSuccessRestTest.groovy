package com.browserup.bup.proxy.rest.assertion.entries.status

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.rest.BaseRestTest
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpResponseDecorator
import org.apache.http.HttpStatus
import org.hamcrest.Matchers
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.*

class EntriesAssertStatusSuccessRestTest extends BaseRestTest {
    def urlOfMostRecentRequest = 'url-most-recent'
    def urlOfOldRequest = 'url-old'
    def urlOfNotToMatchRequest = 'not-to-match'
    def urlPatternToMatchUrl = '.*url-.*'
    def urlPatternNotToMatchUrl = '.*does_not_match-.*'
    def successStatus = HttpStatus.SC_OK
    def nonSuccessStatus = HttpStatus.SC_BAD_REQUEST
    def statusOfNotToMatchUrl = HttpStatus.SC_INTERNAL_SERVER_ERROR
    def responseBody = "success"

    @Override
    String getUrlPath() {
        return 'har/entries/assertStatusSuccess'
    }

    @Test
    void getBadRequestIfUrlPatternIsInvalid() {
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
    void statusSuccessForFilteredResponsesPasses() {
        sendRequestsToTargetServer(successStatus, successStatus, statusOfNotToMatchUrl)

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get all entries found by url pattern', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void statusSuccessForAllResponsesPasses() {
        sendRequestsToTargetServer(successStatus, successStatus, successStatus)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get all assertion entries', assertionResult.requests, Matchers.hasSize(3))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void statusSuccessForAllResponsesFails() {
        sendRequestsToTargetServer(successStatus, successStatus, statusOfNotToMatchUrl)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get all assertion entries', assertionResult.requests, Matchers.hasSize(3))
                assertAssertionFailed(assertionResult)
            }
        }
    }

    @Test
    void statusSuccessForFilteredResponsesFails() {
        sendRequestsToTargetServer(successStatus, nonSuccessStatus, statusOfNotToMatchUrl)

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get all entries found by url pattern', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionFailed(assertionResult)

                def failedRequest = assertionResult.requests.find { it.failed }

                assertTrue('Expected failed assertion entry result has "true" failed flag', failedRequest.failed)
            }
        }
    }

    @Test
    void getEmptyResultIfNoEntryFoundByUrlPattern() {
        sendRequestsToTargetServer(successStatus, nonSuccessStatus, statusOfNotToMatchUrl)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPatternNotToMatchUrl]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get no assertion result entries', assertionResult.requests, Matchers.hasSize(0))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    private void sendRequestsToTargetServer(int oldStatus, int recentStatus, int statusOfNotToMatchUrl) {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, recentStatus)
        mockTargetServerResponse(urlOfOldRequest, responseBody, oldStatus)
        mockTargetServerResponse(urlOfNotToMatchRequest, responseBody, statusOfNotToMatchUrl)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlOfOldRequest, responseBody)
        requestToTargetServer(urlOfMostRecentRequest, responseBody)
        requestToTargetServer(urlOfNotToMatchRequest, responseBody)
    }

    protected void mockTargetServerResponse(String url, String responseBody, int status) {
        def response = aResponse().withStatus(status)
                .withBody(responseBody)
                .withHeader('Content-Type', 'text/plain')
        stubFor(get(urlEqualTo("/${url}")).willReturn(response))
    }
}
