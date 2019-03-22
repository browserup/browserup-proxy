package com.browserup.bup.proxy.rest

import com.browserup.bup.proxy.rest.WithRunningProxyRestTest
import groovyx.net.http.Method
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.junit.Test
import org.mockserver.model.Delay

import static org.junit.Assert.assertEquals

abstract class BaseRestTest extends WithRunningProxyRestTest {
    protected static final Delay TARGET_SERVER_RESPONSE_DELAY = Delay.milliseconds(500)
    protected static final Delay TARGET_SERVER_SLOW_RESPONSE_DELAY = Delay.milliseconds(1000)
    protected static final long SUCCESSFUL_ASSERTION_TIME_WITHIN = TARGET_SERVER_RESPONSE_DELAY.value + 100
    protected static final long FAILED_ASSERTION_TIME_WITHIN = TARGET_SERVER_RESPONSE_DELAY.value - 100
    protected static final int MILLISECONDS_BETWEEN_REQUESTS = 100

    abstract String getUrlPath();

    @Test
    void getBadRequestIfUrlPatternNotProvided() {
        proxyManager.get()[0].newHar()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
    }

    @Test
    void getBadRequestIfUrlPatternIsInvalid() {
        proxyManager.get()[0].newHar()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.query = [urlPattern: '[', milliseconds: '123']
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
        }
    }
}
