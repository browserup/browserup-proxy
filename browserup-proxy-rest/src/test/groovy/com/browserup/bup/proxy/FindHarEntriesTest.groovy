package com.browserup.bup.proxy

import com.browserup.bup.proxy.test.util.ProxyResourceTest
import com.google.sitebricks.headless.Reply
import com.google.sitebricks.headless.Request
import groovyx.net.http.Method
import org.apache.http.HttpHeaders
import org.apache.http.entity.ContentType
import org.awaitility.Awaitility
import org.junit.Assert
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class FindHarEntriesTest extends ProxyResourceTest {

    @Test
    void findHarEntryByUrlPattern() {
        def urlToCatch = "test"
        def urlNotToCatch = "missing"
        def responseBody = "success"

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${urlToCatch}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header(HttpHeaders.CONTENT_TYPE, "text/plain"))
                .withBody(responseBody))

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${urlNotToCatch}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header(HttpHeaders.CONTENT_TYPE, "text/plain"))
                .withBody(responseBody))

        proxyManager.get(proxyPort).newHar()

        def responsesCount = new AtomicInteger()
        httpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlToCatch}"

            response.success = { resp, reader ->
                assertEquals(responseBody, reader.text)

                responsesCount.incrementAndGet()
            }
        }
        httpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlNotToCatch}"

            response.success = { resp, reader ->
                assertEquals(responseBody, reader.text)

                responsesCount.incrementAndGet()
            }
        }

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 2 })

        Reply<?>[] entries = proxyResource.findEntries(proxyPort, createMockedRequest(urlToCatch))

        Assert.assertTrue(entries[0].entity[0].request.url.endsWith(urlToCatch))
        Assert.assertFalse(entries[0].entity[0].request.url.endsWith(urlNotToCatch))
    }

    Request createMockedRequest(urlParam) {
        def mockRestRequest = mock(Request)
        when(mockRestRequest.method()).thenReturn("GET")
        when(mockRestRequest.param("urlPattern")).thenReturn(".*${urlParam}.*".toString())
        mockRestRequest
    }
}