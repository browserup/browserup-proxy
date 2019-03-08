package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.HttpHeaders
import org.apache.http.entity.ContentType
import org.awaitility.Awaitility
import org.hamcrest.Matchers
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class FindHarEntriesRestTest extends WithRunningProxyRestTest {

    @Test
    void findHarEntryByUrlPattern() {
        def urlToCatch = "test"
        def urlNotToCatch = "missing"
        def responseBody = "success"

        mockTargetServerResponse(urlToCatch, responseBody)
        mockTargetServerResponse(urlNotToCatch, responseBody)

        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        targetServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlToCatch}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }
        targetServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlNotToCatch}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 2 })

        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlToCatch}"
            uri.path = "/proxy/${proxy.port}/har/entries"
            uri.query = [url: urlPattern]
            response.success = { _, reader ->
                HarEntry[] entries = new ObjectMapper().readValue(reader, HarEntry[]) as HarEntry[]
                assertThat('Expected to find only one entry', entries, Matchers.arrayWithSize(1))
                assertThat('Expected to find entry containing url from url filter pattern',
                        entries[0].request.url, Matchers.containsString(urlToCatch))
                assertThat('Expected to find no entries with urlNotToCatch filter',
                        entries[0].request.url, Matchers.not(Matchers.containsString(urlNotToCatch)))
            }
        }
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
