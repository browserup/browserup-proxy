package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import java.util.concurrent.atomic.AtomicInteger

import static org.awaitility.Awaitility.await
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

class FindHarEntriesRestTest extends WithRunningProxyRestTest {
    private static final String URL_PATH = 'har/entries'

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

        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })
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
    void findHarEntryByUrlPattern() {
        def urlToCatch = 'test'
        def urlNotToCatch = 'missing'
        def responseBody = 'success'

        mockTargetServerResponse(urlToCatch, responseBody)
        mockTargetServerResponse(urlNotToCatch, responseBody)

        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlToCatch}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }
        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlNotToCatch}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 2 })

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlToCatch}"
            uri.path = "/proxy/${proxy.port}/${URL_PATH}"
            uri.query = [urlPattern: urlPattern]
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

    @Test
    void getEmptyEntriesArrayIfNoEntriesFoundByUrl() {
        def urlToCatch = 'test'
        def urlNotToCatch = 'missing'
        def responseBody = 'success'

        mockTargetServerResponse(urlNotToCatch, responseBody)

        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlNotToCatch}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlToCatch}"
            uri.path = "/proxy/${proxy.port}/${URL_PATH}"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                HarEntry[] entries = new ObjectMapper().readValue(reader, HarEntry[]) as HarEntry[]
                assertThat('Expected get empty har entries array', entries, Matchers.arrayWithSize(0))
            }
        }
    }
}
