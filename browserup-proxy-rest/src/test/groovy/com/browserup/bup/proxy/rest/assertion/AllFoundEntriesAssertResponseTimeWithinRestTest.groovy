package com.browserup.bup.proxy.rest.assertion

import com.browserup.bup.assertion.model.AssertionResult
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import java.util.concurrent.atomic.AtomicInteger

import static org.awaitility.Awaitility.await
import static org.junit.Assert.*

class AllFoundEntriesAssertResponseTimeWithinRestTest extends BaseAssertResponseTimeWithinRestTest {

    @Override
    String getUrlPath() {
        return 'har/entries/assertResponseTimeWithin'
    }

    @Test
    void someEntriesFailTimeWithinAssertion() {
        def delay = TARGET_SERVER_SLOW_RESPONSE_DELAY
        def successfulAssertionMilliseconds = SUCCESSFUL_ASSERTION_TIME_WITHIN
        def responseBody = 'success'
        def commonUrlPart = 'some-url'

        def fastRange = (1..2)
        def slowRange = (3..4)

        def urlPathCreator = { index -> "${commonUrlPart}-${index}" }

        def fastUrls = fastRange.collect(urlPathCreator)
        def slowUrls = slowRange.collect(urlPathCreator)
        def allUrls = fastUrls + slowUrls

        fastUrls.forEach { mockTargetServerResponse(it, responseBody) }
        slowUrls.forEach { mockTargetServerResponse(it, responseBody, delay) }

        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        allUrls.forEach {
            targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
                uri.path = "/${it}"
                response.success = { _, reader ->
                    assertEquals(responseBody, reader.text)
                    responsesCount.incrementAndGet()
                }
            }
        }

        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == allUrls.size() })
        responsesCount.set(0)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${commonUrlPart}.*"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern, milliseconds: successfulAssertionMilliseconds]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get all assertion entries filtered by url pattern',
                        assertionResult.requests, Matchers.hasSize(allUrls.size()))
                assertFalse('Expected assertion to fail', assertionResult.passed)
                assertTrue('Expected assertion to fail', assertionResult.failed)

                assertionResult.requests.forEach { e ->
                    if (fastUrls.find {e.url.contains(it)}) {
                        assertFalse("Expected entry result for fast response to have failed flag = false", e.failed)
                    }
                    if (slowUrls.find {e.url.contains(it)}) {
                        assertTrue("Expected entry result for slow response to have failed flag = true", e.failed)
                    }
                }
                responsesCount.incrementAndGet()
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })
    }

    @Test
    void emptyResultIfNoEntriesFoundForTimeWithinAssertion() {
        def successfulAssertionMilliseconds = SUCCESSFUL_ASSERTION_TIME_WITHIN
        def responseBody = 'success'
        def url = 'some-url'

        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        mockTargetServerResponse(url, responseBody)
        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${url}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*does-not-match.*"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern, milliseconds: successfulAssertionMilliseconds]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get empty assertion entries found by url pattern',
                        assertionResult.requests, Matchers.hasSize(0))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)

                responsesCount.incrementAndGet()
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 2 })
    }
}