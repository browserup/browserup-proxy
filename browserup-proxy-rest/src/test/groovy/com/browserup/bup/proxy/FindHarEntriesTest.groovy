package com.browserup.bup.proxy

import com.browserup.bup.proxy.test.util.ProxyResourceTest
import com.google.sitebricks.headless.Request
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.junit.Assert
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class FindHarEntriesTest extends ProxyResourceTest {
    @Test
    void testCanModifyRequestHeadersWithJavascript() {
        def urlToCatch = "test"
        def urlNotToCatch = "missing"

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${urlToCatch}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header("Content-Type", "text/plain"))
                .withBody("success"))

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${urlNotToCatch}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header("Content-Type", "text/plain"))
                .withBody("success"))

        HTTPBuilder http = getHttpBuilder()

        proxyManager.get(proxyPort).newHar()

        http.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${urlToCatch}"

            response.success = { resp, reader ->
                assertEquals("success", reader.text)

                def mockRestRequest = mock(Request)
                when(mockRestRequest.method()).thenReturn("GET")
                when(mockRestRequest.param("url")).thenReturn(".*${urlToCatch}.*")

                def foundEntries = proxyResource.findEntries(proxyPort, mockRestRequest)
                Assert.assertTrue("Expected to find one har entry by provided URL regexp", foundEntries.entity.size == 1)
                Assert.assertTrue("Expected to find har entry with proper URL", foundEntries.entity[0].request.url.endsWith("test"))
            }
        }

        //TODO add negative case
    }

    @Override
    String[] getArgs() {
        return ["--use-littleproxy", "true"]
    }
}