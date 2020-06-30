/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.BrowserUpProxyServer
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

class DefaultHarPageTest extends MockServerTest {
    private static final String SUCCESSFUL_RESPONSE_BODY = "success"
    private static final String FIRST_URL = "first-url"

    private MitmProxyServer proxy
    private CloseableHttpClient clientToProxy

    @Before
    void startUp() {
        proxy = new MitmProxyServer()
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
    void defaultPageIsCreatedForSuccessfulRequest() {
        mockResponseForPath(FIRST_URL)

        def firstUrl = "http://localhost:${mockServerPort}/${FIRST_URL}"

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)
        assertEquals("Expected first request entry to have initial page ref", 'Default', proxy.har.log.entries[0].pageref)
        assertThat("Expected 1 page available", proxy.har.log.pages, Matchers.hasSize(1))
    }

    @Test
    void defaultPageIsCreatedForHarEntryIfConnectionFailed() {
        mockResponseForPath(FIRST_URL)
        def nonResponsivePort = mockServerPort + 1

        def firstUrl = "http://localhost:${nonResponsivePort}/${FIRST_URL}"

        toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).entity.content)
        assertEquals("Expected first request entry to have initial page ref", 'Default', proxy.har.log.entries[0].pageref)
        assertThat("Expected 1 page available", proxy.har.log.pages, Matchers.hasSize(1))
    }

    private mockResponseForPath(String path) {
        stubFor(get(urlEqualTo("/${path}")).willReturn(ok().withBody(SUCCESSFUL_RESPONSE_BODY)))
    }
}
