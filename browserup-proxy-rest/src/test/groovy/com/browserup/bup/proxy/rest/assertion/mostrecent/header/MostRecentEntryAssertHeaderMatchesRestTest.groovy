package com.browserup.bup.proxy.rest.assertion.mostrecent.header

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.rest.BaseRestTest
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import groovyx.net.http.HttpResponseDecorator
import org.hamcrest.Matchers
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.*

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
    def headerValuePatternToMatch = ".*" as String
    def headerValuePatternNotToMatch = ".*${headerNameNotToFind}.*" as String
    def headerNamePatternToMatch = ".*${headerNameToFind}.*" as String
    def headerNamePatternNotToMatch = ".*${headerNameNotToFind}.*" as String
    //Header[] headers = [new Header(headerNameToFind, "header value before ${headerValueToFind} header value after".toString())]
    HttpHeader[] headers = [new HttpHeader(headerNameToFind, "header value before ${headerValueToFind} header value after".toString())]

    @Override
    String getUrlPath() {
        return 'har/mostRecentEntry/assertResponseHeaderMatches'
    }

    @Test
    void anyNameAndMatchingValuePatternPass() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern, headerValuePattern: headerValuePatternToMatch]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
                
                assertFalse('Expected assertion entry result to have "false" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void anyNameAndNotMatchingValuePatternFail() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPattern, headerValuePattern: headerValuePatternNotToMatch]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionFailed(assertionResult)

                assertTrue('Expected assertion entry result to have "true" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void matchingNameAndMatchingValuePass() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: urlPattern,
                    headerNamePattern: headerNamePatternToMatch,
                    headerValuePattern: headerValuePatternToMatch]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
                
                assertFalse('Expected assertion entry result to have "false" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void notMatchingNameAndMatchingValuePass() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: urlPattern,
                    headerNamePattern: headerNamePatternNotToMatch,
                    headerValuePattern: headerValuePatternToMatch]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
                
                assertFalse('Expected assertion entry result to have "false" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void matchingNameAndNotMatchingValueFail() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: urlPattern,
                    headerNamePattern: headerNamePatternToMatch,
                    headerValuePattern: headerValuePatternNotToMatch]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionFailed(assertionResult)

                assertTrue('Expected assertion entry result to have "true" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void notMatchingNameAndNotMatchingValuePass() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: urlPattern,
                    headerNamePattern: headerNamePatternNotToMatch,
                    headerValuePattern: headerValuePatternNotToMatch]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
                
                assertFalse('Expected assertion entry result to have "false" failed flag', assertionResult.requests[0].failed)
            }
        }
    }

    @Test
    void emptyResultIfNoEntryFoundByUrlPattern() {
        sendRequestsToTargetServer()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [urlPattern: urlPatternNotToMatchUrl, headerValuePattern: headerValueNotToFind]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get no assertion result entries', assertionResult.requests, Matchers.hasSize(0))
                assertAssertionPassed(assertionResult)
                
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

    protected void mockTargetServerResponse(String url, String responseBody, HttpHeader[] headers) {
        def allHeaders = headers + [new HttpHeader('Content-Type', 'text/plain')] as HttpHeader[]
        def response = ok()
                .withBody(responseBody)
                .withHeaders(new HttpHeaders(allHeaders))
        stubFor(get(urlEqualTo("/${url}")).willReturn(response))
    }
}
