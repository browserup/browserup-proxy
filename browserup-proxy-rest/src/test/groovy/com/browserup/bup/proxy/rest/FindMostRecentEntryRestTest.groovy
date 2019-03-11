package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.awaitility.Awaitility
import org.hamcrest.Matchers
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class FindMostRecentEntryRestTest extends WithRunningProxyRestTest {

    @Test
    void getBadRequestIfUrlPatternNotProvided() {
        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/har/mostRecentEntry"
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
            uri.query = [urlPattern: '[']
            uri.path = "/proxy/${proxy.port}/har/mostRecentEntry"
            response.failure = { resp, reader ->
                responsesCount.incrementAndGet()
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 1 })
    }

    @Test
    void findMostRecentHarEntryByUrlPattern() {
        def urlOfMostRecentRequest = 'url-most-recent'
        def urlOfOldRequest = 'url-old'
        def commonUrlPattern = '.*url-.*'
        def responseBody = 'success'

        mockTargetServerResponse(urlOfMostRecentRequest, responseBody)
        mockTargetServerResponse(urlOfOldRequest, responseBody)

        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        targetServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfOldRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        Thread.sleep(1000)

        targetServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfMostRecentRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 2 })

        def allCapturedEntries = [] as HarEntry[]
        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/har/entries"
            uri.query = [urlPattern: commonUrlPattern]
            response.success = { _, reader ->
                allCapturedEntries = (new ObjectMapper().readValue(reader, HarEntry[]) as HarEntry[])
                assertThat('Expected to find both entries', allCapturedEntries, Matchers.arrayWithSize(2))
                responsesCount.incrementAndGet()
            }
        }

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 3 })

        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${commonUrlPattern}"
            uri.path = "/proxy/${proxy.port}/har/mostRecentEntry"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def actualEntry = new ObjectMapper().readValue(reader, HarEntry) as HarEntry
                def expectedMostRecentEntry = allCapturedEntries.max {it.startedDateTime}
                assertNotNull('Expected to find an entry', actualEntry)
                assertThat('Expected to find most recent entry containing url from url filter pattern',
                        actualEntry.request.url, Matchers.containsString(urlOfMostRecentRequest))
                assertThat('Expected that found entry has maximum started date time',
                        actualEntry.startedDateTime, Matchers.equalTo(expectedMostRecentEntry.startedDateTime))
            }
        }
    }

    @Test
    void getEmptyEntryIfNoEntryFoundByUrlPattern() {
        def urlOfMostRecentRequest = 'url-most-recent'
        def urlOfOldRequest = 'url-old'
        def urlPattern = '.*does-not-match-.*'
        def responseBody = 'success'

        mockTargetServerResponse(urlOfMostRecentRequest, responseBody)
        mockTargetServerResponse(urlOfOldRequest, responseBody)

        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        targetServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfOldRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        Thread.sleep(1000)

        targetServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfMostRecentRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 2 })

        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/har/mostRecentEntry"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def actualEntry = new ObjectMapper().readValue(reader, HarEntry) as HarEntry
                assertNull('Expected to find empty entry', actualEntry.startedDateTime)
            }
        }
    }

    @Test
    void returnEmptyEntriesArrayIfNoEntriesFoundByUrl() {
        def urlToCatch = 'test'
        def urlNotToCatch = 'missing'
        def responseBody = 'success'

        mockTargetServerResponse(urlNotToCatch, responseBody)

        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        targetServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlNotToCatch}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 1 })

        proxyRestServerHttpBuilder.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlToCatch}"
            uri.path = "/proxy/${proxy.port}/har/entries"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                HarEntry[] entries = new ObjectMapper().readValue(reader, HarEntry[]) as HarEntry[]
                assertThat('Expected get empty har entries array', entries, Matchers.arrayWithSize(0))
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
