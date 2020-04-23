/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.*

class DefaultStepIdTest extends MockServerTest {
    private static final String SUCCESSFUL_RESPONSE_BODY = "success"
    private static final String FIRST_URL = "first-url"
    private static final String SECOND_URL = "second-url"
    private static final String THIRD_URL = "third-url"
    private static final String INITIAL_STEP_NAME = "Step Name"
    private static final String DEFAULT_STEP_NAME = "Default"

    private MitmProxyServer proxy
    private CloseableHttpClient clientToProxy

    @Before
    void startUp() {
        proxy = new MitmProxyServer()
        proxy.start()

        clientToProxy = NewProxyServerTestUtil.getNewHttpClient(proxy.port)
    }

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testDefaultStepIdIfNoCurrentPage() {
        proxy.newHar(INITIAL_STEP_NAME)

        sleep(2000)

        mockResponseForPath(FIRST_URL)
        mockResponseForPath(SECOND_URL)
        mockResponseForPath(THIRD_URL)

        def firstUrl = "http://localhost:${mockServerPort}/${FIRST_URL}"

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)
        assertEquals("Expected first request entry to have initial page ref", INITIAL_STEP_NAME, proxy.har.log.entries[0].pageref)
        assertThat("Expected 1 page available", proxy.har.log.pages, Matchers.hasSize(1))

        proxy.endPage()

        sleep 10

        def secondUrl = "http://localhost:${mockServerPort}/${SECOND_URL}"

        def respBody2 = toStringAndClose(clientToProxy.execute(new HttpGet(secondUrl)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody2)
        def defaultPage = proxy.har.log.pages.find {it.id == DEFAULT_STEP_NAME}
        assertNotNull("Expected not null default page", defaultPage)
        assertNotNull("Expected page with started date time", defaultPage.startedDateTime)
        def newestEntry = proxy.har.log.entries.sort({a,b -> (b.startedDateTime <=> a.startedDateTime) }).first()
        assertEquals("Expected to get default step name ", DEFAULT_STEP_NAME, newestEntry.pageref)

        def thirdUrl = "http://localhost:${mockServerPort}/${THIRD_URL}"

        def respBody3 = toStringAndClose(clientToProxy.execute(new HttpGet(thirdUrl)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody3)

        assertThat("Expected two pages available", proxy.har.log.pages, Matchers.hasSize(2))
        assertNotNull("Expected to find default page among pages", proxy.har.log.pages.find {it.id = DEFAULT_STEP_NAME})
    }

    @Test
    void testHarIsCreatedAfterFirstRequestIfNoNewHarCalled() {
        mockResponseForPath(FIRST_URL)

        assertNull("Expected null har before any requests sent", proxy.har)

        def firstUrl = "http://localhost:${mockServerPort}/${FIRST_URL}"

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        assertNotNull("Expected non null har after request sent", proxy.har)
    }

    @Test
    void testEntryCapturedIfNoNewHarCalled() {
        mockResponseForPath(FIRST_URL)

        def firstUrl = "http://localhost:${mockServerPort}/${FIRST_URL}"

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        assertThat("Expected to get one entry", proxy.har.log.entries, Matchers.hasSize(1))
        assertEquals("Expected entry to have default page ref", proxy.har.log.entries[0].pageref, DEFAULT_STEP_NAME)
    }

    @Test
    void testEntryWithDefaultPageRefRemovedAfterNewHarCreated() {
        mockResponseForPath(FIRST_URL)

        def firstUrl = "http://localhost:${mockServerPort}/${FIRST_URL}"

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        assertThat("Expected to get one entry", proxy.har.log.entries, Matchers.hasSize(1))
        assertEquals("Expected entry to have default page ref", proxy.har.log.entries[0].pageref, DEFAULT_STEP_NAME)

        proxy.newHar(INITIAL_STEP_NAME)
        assertThat("Expected to get no entries after new har called", proxy.har.log.entries, Matchers.hasSize(0))
    }

    private mockResponseForPath(String path) {
        stubFor(get(urlEqualTo("/${path}")).willReturn(ok().withBody(SUCCESSFUL_RESPONSE_BODY)))
    }
}
