/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.browserup.harreader.model.Har
import com.github.tomakehurst.wiremock.http.GenericHttpUriRequest
import org.apache.http.HttpHost
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.After
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

class GetHarTest extends MockServerTest {
    private MitmProxyServer proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testGetHarClean() {
        def stubUrl = "/testCaptureResponseCookiesInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success"))
        )

        proxy = new MitmProxyServer()
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_COOKIES] as Set)
        proxy.setTrustAllServers(true)
        proxy.start()

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/testCaptureResponseCookiesInHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))
        har = proxy.getHar(true)
        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        har = proxy.getHar()
        assertThat("Expected to find no entries in the HAR", har.getLog().getEntries(), empty())

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }


}
