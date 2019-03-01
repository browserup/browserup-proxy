package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.har.HarEntriesUrlPatternFilter
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.browserup.harreader.model.Har
import com.browserup.harreader.model.HarEntry
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.mockserver.matchers.Times

import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertEquals
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class HarEntriesFiltersTest extends MockServerTest {
    private static final String SUCCESSFUL_RESPONSE_BODY = "success"

    BrowserUpProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testHarEntriesFilter() {
        String urlPathToPassFilter = "first-url"
        String urlPathNotToPassFilter = "second-url"

        mockSuccessfulResponseForPath(urlPathToPassFilter)
        mockSuccessfulResponseForPath(urlPathNotToPassFilter)

        proxy = new BrowserUpProxyServer()
        proxy.start()

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String requestUrlToPassFilter = "http://localhost:${mockServerPort}/${urlPathToPassFilter}"
            String requestUrlNotToPassFilter = "http://localhost:${mockServerPort}/${urlPathNotToPassFilter}"

            def respBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(requestUrlToPassFilter)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

            respBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(requestUrlNotToPassFilter)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        List<HarEntry> filteredEntries = proxy.getFilteredHarEntries(new HarEntriesUrlPatternFilter(".*${urlPathToPassFilter}?"))

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))
        assertTrue("Expected to get one filtered Har entry", filteredEntries.size() == 1)
        assertTrue(
                "Expected to get one filtered Har entry with url path: '${urlPathToPassFilter}'",
                filteredEntries.get(0).getRequest().getUrl().contains(urlPathToPassFilter))
        assertFalse(
                "Expected not to get Har entry with url path: '${urlPathNotToPassFilter}'",
                filteredEntries.get(0).getRequest().getUrl().contains(urlPathNotToPassFilter))
    }

    private mockSuccessfulResponseForPath(String path) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${path}"),
                Times.once())
                .respond(response()
                .withStatusCode(200)
                .withBody(SUCCESSFUL_RESPONSE_BODY))
    }
}
