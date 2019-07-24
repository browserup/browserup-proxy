/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.Har
import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat

class ValidateHarRestTest extends BaseRestTest {

    @Override
    String getUrlPath() {
        return 'har'
    }

    @Test
    void validateHarForRequestWithEmptyContentAndMimeType() {
        def urlToCatch = 'test'
        def responseBody = ''

        mockTargetServerResponse(urlToCatch, responseBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlToCatch, responseBody)

        proxyRestServerClient.request(Method.GET, ContentType.WILDCARD) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            response.success = { HttpResponseDecorator resp ->
                Har har = new ObjectMapper().readValue(resp.entity.content, Har) as Har
                assertNull("Expected null browser", har.log.browser)
                assertEquals("Expected zero content size for the entry", 0, har.log.entries[0].response.content.size)
                assertEquals("Expected empty redirect url", "", har.log.entries[0].response.redirectURL)
                assertEquals("Expected empty mime type", "", har.log.entries[0].response.content.mimeType)
                assertEquals("Expected empty request post data mime type", "", har.log.entries[0].request.postData.mimeType)
            }
        }

        WireMock.verify(1, getRequestedFor(urlEqualTo("/${urlToCatch}")))
    }

    protected void mockTargetServerResponse(String url, String responseBody) {
        def response = aResponse().withStatus(200)
                .withBody(responseBody)
                .withHeader('Content-Type', '')
        stubFor(get(urlEqualTo("/${url}")).willReturn(response))
    }
}
