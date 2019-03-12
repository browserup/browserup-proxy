package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.awaitility.Awaitility.await
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat

class FindMostRecentEntryRestTest extends WithRunningProxyRestTest {
    private static final String URL_PATH = 'har/mostRecentEntry'
    private static final int MILLISECONDS_BETWEEN_REQUESTS = 100

    @Test
    void getBadRequestIfUrlPatternNotProvided() {
        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${URL_PATH}"
            response.failure = { resp, reader ->
                responsesCount.incrementAndGet()
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }

        await().atMost(5, TimeUnit.SECONDS).until({ -> responsesCount.get() == 1 })
    }

    @Test
    void getBadRequestIfUrlPatternIsInvalid() {
        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.query = [urlPattern: '[']
            uri.path = "/proxy/${proxy.port}/${URL_PATH}"
            response.failure = { resp, reader ->
                responsesCount.incrementAndGet()
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })
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

        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfOldRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        sleep MILLISECONDS_BETWEEN_REQUESTS

        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfMostRecentRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 2 })

        def allCapturedEntries = [] as HarEntry[]
        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/har/entries"
            uri.query = [urlPattern: commonUrlPattern]
            response.success = { _, reader ->
                allCapturedEntries = (new ObjectMapper().readValue(reader, HarEntry[]) as HarEntry[])
                assertThat('Expected to find both entries', allCapturedEntries, Matchers.arrayWithSize(2))
                responsesCount.incrementAndGet()
            }
        }

        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 3 })

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${commonUrlPattern}"
            uri.path = "/proxy/${proxy.port}/${URL_PATH}"
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

        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfOldRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        sleep MILLISECONDS_BETWEEN_REQUESTS

        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfMostRecentRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 2 })

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${URL_PATH}"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def actualEntry = new ObjectMapper().readValue(reader, HarEntry) as HarEntry
                assertNull('Expected to find empty entry', actualEntry.startedDateTime)
            }
        }
    }
}
