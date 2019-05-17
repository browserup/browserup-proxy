package com.browserup.bup.proxy.rest.assertion.entries

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.rest.BaseRestTest
import com.browserup.harreader.model.HttpMethod
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.junit.Assert.*
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class EntriesAssertStatusRedirectionRestTest extends BaseRestTest {
    def urlOfMostRecentRequest = 'url-most-recent'
    def urlOfOldRequest = 'url-old'
    def urlOfNotToMatchRequest = 'not-to-match'
    def urlPatternToMatchUrl = '.*url-.*'
    def urlPatternNotToMatchUrl = '.*does_not_match-.*'
    def redirectionStatus = HttpStatus.SC_USE_PROXY
    def nonRedirectionStatus = HttpStatus.SC_BAD_REQUEST
    def statusOfNotToMatchUrl = HttpStatus.SC_INTERNAL_SERVER_ERROR
    def responseBody = "success"

    @Override
    String getUrlPath() {
        return 'har/entries/assertStatusRedirection'
    }

    @Test
    void getBadRequestIfUrlPatternIsInvalid() {
        proxyManager.get()[0].newHar()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: '[']
            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
    }

    @Test
    void statusRedirectionForFilteredResponsesPasses() {
        sendRequestsToTargetServer(redirectionStatus, redirectionStatus, statusOfNotToMatchUrl)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get all entries found by url pattern', assertionResult.requests, Matchers.hasSize(2))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
            }
        }
    }

    @Test
    void statusRedirectionForAllResponsesPasses() {
        sendRequestsToTargetServer(redirectionStatus, redirectionStatus, redirectionStatus)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get all assertion entries', assertionResult.requests, Matchers.hasSize(3))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
            }
        }
    }

    @Test
    void statusRedirectionForAllResponsesFails() {
        sendRequestsToTargetServer(redirectionStatus, redirectionStatus, statusOfNotToMatchUrl)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get all assertion entries', assertionResult.requests, Matchers.hasSize(3))
                assertFalse('Expected assertion to fail', assertionResult.passed)
                assertTrue('Expected assertion to fail', assertionResult.failed)
            }
        }
    }

    @Test
    void statusRedirectionForFilteredResponsesFails() {
        sendRequestsToTargetServer(redirectionStatus, nonRedirectionStatus, statusOfNotToMatchUrl)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get all entries found by url pattern', assertionResult.requests, Matchers.hasSize(2))
                assertFalse('Expected assertion to fail', assertionResult.passed)
                assertTrue('Expected assertion to fail', assertionResult.failed)

                def failedRequest = assertionResult.requests.find { it.failed }

                assertTrue('Expected failed assertion entry result has "true" failed flag', failedRequest.failed)
            }
        }
    }

    @Test
    void getEmptyResultIfNoEntryFoundByUrlPattern() {
        sendRequestsToTargetServer(redirectionStatus, nonRedirectionStatus, statusOfNotToMatchUrl)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPatternNotToMatchUrl]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get no assertion result entries', assertionResult.requests, Matchers.hasSize(0))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
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
        targetMockedServer.when(request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/${url}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(status)
                .withHeader(new Header(HttpHeaders.CONTENT_TYPE, 'text/plain'))
                .withBody(responseBody))
    }
}
