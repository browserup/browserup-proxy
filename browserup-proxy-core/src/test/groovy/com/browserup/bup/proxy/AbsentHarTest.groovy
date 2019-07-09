/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockserver.matchers.Times

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class AbsentHarTest extends MockServerTest {
    private static final String SUCCESSFUL_RESPONSE_BODY = "success"
    private static final String FIRST_URL = "first-url"
    private static final String SECOND_URL = "second-url"
    private static final String THIRD_URL = "third-url"
    private static final String INITIAL_STEP_NAME = "Step Name"
    private static final String DEFAULT_STEP_NAME = "Default"

    private BrowserUpProxyServer proxy
    private CloseableHttpClient clientToProxy

    @Before
    void startUp() {
        proxy = new BrowserUpProxyServer()
        proxy.setTrustAllServers(true)
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
    void noHarAvailableCallNewPageCheckHarCreatedAndRequestsLogged() {
        proxy.newPage(INITIAL_STEP_NAME, INITIAL_STEP_NAME)

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

        def newestEntry = proxy.har.log.entries.sort({a,b -> (b.startedDateTime <=> a.startedDateTime) }).first()
        assertEquals("Expected to get default step name ", DEFAULT_STEP_NAME, newestEntry.pageref)

        def thirdUrl = "http://localhost:${mockServerPort}/${THIRD_URL}"

        def respBody3 = toStringAndClose(clientToProxy.execute(new HttpGet(thirdUrl)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody3)

        assertThat("Expected two pages available", proxy.har.log.pages, Matchers.hasSize(2))
        assertNotNull("Expected to find default page among pages", proxy.har.log.pages.find {it.id = DEFAULT_STEP_NAME})
    }

    @Test
    void noHarAvailableCallEndPage() {
        proxy.endPage()

        //No exception thrown
    }

    private mockResponseForPath(String path) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${path}"),
                Times.once())
                .respond(response()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(SUCCESSFUL_RESPONSE_BODY))
    }
}
