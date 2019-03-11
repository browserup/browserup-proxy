package com.browserup.bup.proxy.rest

import groovyx.net.http.Method
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.awaitility.Awaitility
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.junit.Assert.assertEquals
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class MostRecentEntryAssertResponseTimeWithinRestTest extends WithRunningProxyRestTest {

    @Test
    void getBadRequestIfUrlPatternNotProvided() {
        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/har/mostRecentEntry/assertResponseTimeWithin"
            uri.query = [milliseconds: '123']
            response.failure = { resp, reader ->
                responsesCount.incrementAndGet()
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 1 })
    }

    @Test
    void getBadRequestIfUrlPatternIsInvalid() {
        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.query = [urlPattern: '[', milliseconds: '123']
            uri.path = "/proxy/${proxy.port}/har/mostRecentEntry/assertResponseTimeWithin"
            response.failure = { resp, reader ->
                responsesCount.incrementAndGet()
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 1 })
    }

    @Test
    void getBadRequestIfMillisecondsNotValid() {
        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.query = [urlPattern: '.*', milliseconds: 'abcd']
            uri.path = "/proxy/${proxy.port}/har/mostRecentEntry/assertResponseTimeWithin"
            response.failure = { resp, reader ->
                responsesCount.incrementAndGet()
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 1 })
    }


    private void mockTargetServerResponse(String url, String responseBody) {
        targetMockedServer.when(request()
                .withMethod("GET")
                .withPath("/${url}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header(HttpHeaders.CONTENT_TYPE, "text/plain"))
                .withBody(responseBody))
    }
}
