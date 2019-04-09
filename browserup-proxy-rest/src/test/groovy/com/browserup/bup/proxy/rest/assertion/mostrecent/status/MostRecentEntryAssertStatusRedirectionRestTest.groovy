package com.browserup.bup.proxy.rest.assertion.mostrecent.status

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.rest.BaseRestTest
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.HttpHeaders
import groovyx.net.http.Method
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static org.junit.Assert.*

class MostRecentEntryAssertStatusRedirectionRestTest extends BaseRestTest {
    def urlOfMostRecentRequest = 'url-most-recent'
    def urlOfOldRequest = 'url-old'
    def urlPatternToMatchUrl = '.*url-.*'
    def urlPatternNotToMatchUrl = '.*does_not_match-.*'
    def redirectionStatus = HttpStatus.SC_TEMPORARY_REDIRECT
    def nonRedirectionStatus = HttpStatus.SC_BAD_REQUEST
    def responseBody = "success"

    @Override
    String getUrlPath() {
        return 'har/mostRecentEntry/assertStatusRedirection'
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
    void statusRedirectionPasses() {
        sendRequestsToTargetServer(nonRedirectionStatus, redirectionStatus)

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
    void statusRedirectionFails() {
        sendRequestsToTargetServer(redirectionStatus, nonRedirectionStatus)

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
        sendRequestsToTargetServer(redirectionStatus, nonRedirectionStatus)

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
        mockTargetServerResponse(urlOfMostRecentRequest, recentStatus)
        mockTargetServerResponse(urlOfOldRequest, oldStatus)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlOfOldRequest)

        sleep MILLISECONDS_BETWEEN_REQUESTS

        requestToTargetServer(urlOfMostRecentRequest)
    }

    void requestToTargetServer(url) {
        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${url}"
            response.success = { _, reader ->   }
            response.failure = { _, reader ->   }
        }
    }

    protected void mockTargetServerResponse(String url, int status) {
        stubFor(get(urlMatching('.*test.com.*')).willReturn(ok()))
        stubFor(get(urlEqualTo("/${url}")).
                willReturn(
                        ok().
                                withStatus(status).
                                withHeader(HttpHeaders.CONTENT_TYPE, 'text/plain').
                                withHeader(HttpHeaders.LOCATION, 'test.com').
                                withBody(responseBody))
        )
    }
}
