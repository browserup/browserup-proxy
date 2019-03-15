package com.browserup.bup.proxy.rest.assertion

import com.browserup.bup.proxy.rest.WithRunningProxyRestTest
import groovyx.net.http.Method
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.awaitility.Duration
import org.junit.Test
import org.mockserver.model.Delay

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.awaitility.Awaitility.await
import static org.junit.Assert.assertEquals

abstract class BaseAssertResponseTimeWithinRestTest extends WithRunningProxyRestTest {
    protected static final Delay TARGET_SERVER_RESPONSE_DELAY = Delay.milliseconds(500)
    protected static final Delay TARGET_SERVER_SLOW_RESPONSE_DELAY = Delay.milliseconds(1000)
    protected static final long SUCCESSFUL_ASSERTION_TIME_WITHIN = TARGET_SERVER_RESPONSE_DELAY.value + 100
    protected static final long FAILED_ASSERTION_TIME_WITHIN = TARGET_SERVER_RESPONSE_DELAY.value - 100
    protected static final int MILLISECONDS_BETWEEN_REQUESTS = 100
    protected static final Duration WAIT_FOR_RESPONSE_DURATION = new Duration(5, TimeUnit.SECONDS)

    abstract String getUrlPath();

    @Test
    void getBadRequestIfUrlPatternNotProvided() {
        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [milliseconds: '123']
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
            uri.query = [urlPattern: '[', milliseconds: '123']
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            response.failure = { resp, reader ->
                responsesCount.incrementAndGet()
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })
    }

    @Test
    void getBadRequestIfMillisecondsNotValid() {
        proxyManager.get()[0].newHar()

        def responsesCount = new AtomicInteger()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.query = [urlPattern: '.*', milliseconds: 'abcd']
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            response.failure = { resp, reader ->
                responsesCount.incrementAndGet()
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
        await().atMost(WAIT_FOR_RESPONSE_DURATION).until({ -> responsesCount.get() == 1 })
    }
}
