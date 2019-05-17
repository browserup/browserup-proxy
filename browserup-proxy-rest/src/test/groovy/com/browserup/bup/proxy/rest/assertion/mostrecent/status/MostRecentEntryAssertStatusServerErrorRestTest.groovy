package com.browserup.bup.proxy.rest.assertion.mostrecent.status

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

class MostRecentEntryAssertStatusServerErrorRestTest extends BaseRestTest {
    def urlOfMostRecentRequest = 'url-most-recent'
    def urlOfOldRequest = 'url-old'
    def urlPatternToMatchUrl = '.*url-.*'
    def urlPatternNotToMatchUrl = '.*does_not_match-.*'
    def serverErrorStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
    def nonServerErrorStatus = HttpStatus.SC_OK
    def responseBody = "success"

    @Override
    String getUrlPath() {
        return 'har/mostRecentEntry/assertStatusServerError'
    }

    @Test
    void getBadRequestUrlPatternIsInvalid() {
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
    void statusServerErrorPasses() {
        sendRequestsToTargetServer(nonServerErrorStatus, serverErrorStatus)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
                assertFalse('Expected assertion entry result to have "false" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void statusServerErrorFails() {
        sendRequestsToTargetServer(serverErrorStatus, nonServerErrorStatus)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertFalse('Expected assertion to fail', assertionResult.passed)
                assertTrue('Expected assertion to fail', assertionResult.failed)
                assertTrue('Expected assertion entry result to have "true" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void getEmptyResultIfNoEntryFoundByUrlPattern() {
        sendRequestsToTargetServer(serverErrorStatus, nonServerErrorStatus)

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
        targetMockedServer.when(request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/${url}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(status)
                .withHeaders(
                new Header(HttpHeaders.CONTENT_TYPE, 'text/plain'),
                new Header(HttpHeaders.LOCATION, 'test.com')
        ))
    }
}
