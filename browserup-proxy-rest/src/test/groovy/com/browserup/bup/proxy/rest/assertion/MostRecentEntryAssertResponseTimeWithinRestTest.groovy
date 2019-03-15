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

class MostRecentEntryAssertResponseTimeWithinRestTest extends BaseAssertResponseTimeWithinRestTest {

    @Override
    String getUrlPath() {
        return 'har/mostRecentEntry/assertResponseTimeWithin'
    }

    @Test
    void passAndFailTimeWithinAssertion() {
        def urlOfMostRecentRequest = 'url-most-recent'
        def delay = TARGET_SERVER_RESPONSE_DELAY
        def successfulAssertionMilliseconds = SUCCESSFUL_ASSERTION_TIME_WITHIN
        def failedAssertionMilliseconds = FAILED_ASSERTION_TIME_WITHIN
        def urlOfOldRequest = 'url-old'
        def commonUrlPattern = '.*url-.*'
        def responseBody = 'success'

        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, delay)
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
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })

        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfMostRecentRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 2 })

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${commonUrlPattern}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern, milliseconds: successfulAssertionMilliseconds]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
                assertFalse('Expected assertion entry result to have "false" failed flag', assertionResult.requests[0].failed)
                responsesCount.incrementAndGet()
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 3 })

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${commonUrlPattern}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern, milliseconds: failedAssertionMilliseconds]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertFalse('Expected assertion to fail', assertionResult.passed)
                assertTrue('Expected assertion to fail', assertionResult.failed)
                assertTrue('Expected assertion entry result to have "true" failed flag', assertionResult.requests[0].failed)
                assertThat('Expected assertion entry result to have "time exceeded" message',
                        assertionResult.requests[0].message, Matchers.containsString('Time exceeded'))
                responsesCount.incrementAndGet()
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 4 })
    }

    @Test
    void getEmptyResultIfNoEntryFoundByUrlPattern() {
        def urlOfMostRecentRequest = 'url-most-recent'
        def delay = TARGET_SERVER_RESPONSE_DELAY
        def assertionMilliseconds = SUCCESSFUL_ASSERTION_TIME_WITHIN
        def urlOfOldRequest = 'url-old'
        def urlPattern = '.*does-not-match.*'
        def responseBody = 'success'

        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, delay)
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
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })

        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlOfMostRecentRequest}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
                responsesCount.incrementAndGet()
            }
        }

        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 2 })

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern, milliseconds: assertionMilliseconds]
            response.success = { _, reader ->
                def assertionResult = new ObjectMapper().readValue(reader, AssertionResult) as AssertionResult
                assertNotNull('Expected to get non null assertion result', assertionResult)
                assertThat('Expected to get no assertion result entries', assertionResult.requests, Matchers.hasSize(0))
                assertTrue('Expected assertion to pass', assertionResult.passed)
                assertFalse('Expected assertion to pass', assertionResult.failed)
                responsesCount.incrementAndGet()
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 3 })
    }
}
