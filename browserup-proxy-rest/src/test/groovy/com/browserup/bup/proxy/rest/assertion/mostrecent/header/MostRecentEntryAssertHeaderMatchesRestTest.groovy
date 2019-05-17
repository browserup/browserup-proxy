package com.browserup.bup.proxy.rest.assertion.mostrecent.header

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.rest.BaseRestTest
import com.browserup.harreader.model.HttpMethod
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.ConnectionOptions
import org.mockserver.model.Header

import static org.junit.Assert.*
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class MostRecentEntryAssertHeaderMatchesRestTest extends BaseRestTest {
    def responseBody = 'success'
    def urlOfMostRecentRequest = 'url-most-recent'
    def urlOfOldRequest = 'url-old'
    def urlPatternToMatchUrl = '.*url-.*'
    def urlPatternNotToMatchUrl = '.*does_not_match-.*'
    def headerValueToFind = 'some-header-part'
    def headerValueNotToFind = 'will not find'
    def headerNameToFind = 'some-header-name'
    def headerNameNotToFind = 'some-header-name-not-to-find'
    def headerValuePatternToMatch = ".*(${headerValueToFind}|\\d+).*" as String
    def headerValuePatternNotToMatch = ".*${headerNameNotToFind}.*" as String
    def headerNamePatternToMatch = ".*${headerNameToFind}.*" as String
    def headerNamePatternNotToMatch = ".*${headerNameNotToFind}.*" as String
    Header[] headers = [new Header(headerNameToFind, "header value before ${headerValueToFind} header value after".toString())]

    @Override
    String getUrlPath() {
        return 'har/mostRecentEntry/assertResponseHeaderMatches'
    }

    @Test
    void anyNameAndMatchingValuePatternPass() {
        sendRequestsToTargetServer()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern, headerValuePattern: headerValuePatternToMatch]
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
    void anyNameAndNotMatchingValuePatternFail() {
        sendRequestsToTargetServer()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern, headerValuePattern: headerValuePatternNotToMatch]
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
    void matchingNameAndMatchingValuePass() {
        sendRequestsToTargetServer()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [
                    urlPattern: urlPattern,
                    headerNamePattern: headerNamePatternToMatch,
                    headerValuePattern: headerValuePatternToMatch]
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
    void notMatchingNameAndMatchingValuePass() {
        sendRequestsToTargetServer()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [
                    urlPattern: urlPattern,
                    headerNamePattern: headerNamePatternNotToMatch,
                    headerValuePattern: headerValuePatternToMatch]
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
    void matchingNameAndNotMatchingValueFail() {
        sendRequestsToTargetServer()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [
                    urlPattern: urlPattern,
                    headerNamePattern: headerNamePatternToMatch,
                    headerValuePattern: headerValuePatternNotToMatch]
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
    void notMatchingNameAndNotMatchingValuePass() {
        sendRequestsToTargetServer()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [
                    urlPattern: urlPattern,
                    headerNamePattern: headerNamePatternNotToMatch,
                    headerValuePattern: headerValuePatternNotToMatch]
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
    void emptyResultIfNoEntryFoundByUrlPattern() {
        sendRequestsToTargetServer()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPatternNotToMatchUrl, headerValuePattern: headerValueNotToFind]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get no assertion result entries', assertionResult.requests, Matchers.hasSize(0))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
            }
        }
    }

    private void sendRequestsToTargetServer() {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_HEADERS)

        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, headers)
        mockTargetServerResponse(urlOfOldRequest, responseBody, headers)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlOfOldRequest, responseBody)

        sleep MILLISECONDS_BETWEEN_REQUESTS

        requestToTargetServer(urlOfMostRecentRequest, responseBody)
    }

    protected void mockTargetServerResponse(String url, String responseBody, Header[] headers) {
        def connectionOptions = ConnectionOptions
                .connectionOptions()
                .withSuppressConnectionHeader(true)

        targetMockedServer.when(request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/${url}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(responseBody)
                .withConnectionOptions(connectionOptions)
                .withHeaders(headers))
    }
}
