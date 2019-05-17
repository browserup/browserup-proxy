package com.browserup.bup.proxy.rest.assertion.entries

import com.browserup.bup.assertion.model.AssertionResult
import com.browserup.bup.proxy.rest.BaseRestTest
import com.browserup.harreader.model.HttpMethod
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_OK
import static org.junit.Assert.*
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class EntriesAssertResourceImagesRestTest extends BaseRestTest {
    def requestUrl = 'request-url'
    def responseBody = "success"

    @Override
    String getUrlPath() {
        return 'har/entries/assertImageResponsesSuccessful'
    }

    @Test
    void noBrokenImagesTestForExpectedMediaTypeAndStatusOk() {
        Header header = new Header("Content-Type", "image/jpg")
        sendRequestsToTargetServer(SC_OK, header)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get all entries found by media type', assertionResult.requests, Matchers.hasSize(1))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
            }
        }
    }

    @Test
    void noBrokenImagesTestForEmptyMediaTypeAndStatusOk() {
        Header header = new Header("Content-Type", "")
        sendRequestsToTargetServer(SC_OK, header)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get all entries found by media type', assertionResult.requests, Matchers.hasSize(0))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
            }
        }
    }

    @Test
    void noBrokenImagesTestForExpectedMediaTypeAndStatusNotFound() {
        Header header = new Header("Content-Type", "image/jpg")
        sendRequestsToTargetServer(SC_NOT_FOUND, header)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"

            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertEquals("Expected response status to be less then: '400', but was: '404'", assertionResult.requests.get(0).message)
                assertFalse('Expected assertion to pass', assertionResult.passed)
                assertTrue('Expected assertion to pass', assertionResult.failed)
            }
        }
    }


    private void sendRequestsToTargetServer(int status, Header header) {
        mockTargetServerResponse(requestUrl, status, header)

        proxyManager.get()[0].newHar()

        requestToTargetServer(requestUrl, responseBody)
    }

    protected void mockTargetServerResponse(String url, int status, Header header) {
        targetMockedServer.when(request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/${url}"),
                Times.once())
                .respond(response()
                        .withStatusCode(status)
                        .withHeader(header)
                        .withBody(responseBody))
    }
}
