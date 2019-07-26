/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.Har
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

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
                assertNotNull("Expected not null log creator name", har.log.creator.name)
                assertNotNull("Expected not null log creator version", har.log.creator.version)

                har.log.pages.each {
                    assertNotNull("Expected not null har log pages id", it.id)
                    assertNotNull("Expected not null har log pages title", it.title)
                    assertNotNull("Expected not null har log pages startedDateTime", it.startedDateTime)
                    assertNotNull("Expected not null har log pages pageTimings", it.pageTimings)
                }

                har.log.entries.each {
                    assertNotNull("Expected not null har entries startedDateTime", it.startedDateTime)
                    assertNotNull("Expected not null har entries time", it.time)
                    assertNotNull("Expected not null har entries request", it.request)
                    assertNotNull("Expected not null har entries response", it.response)
                    assertNotNull("Expected not null har entries cache", it.cache)
                    assertNotNull("Expected not null har entries timings", it.timings)

                    assertNotNull("Expected not null har entries requests method", it.request.method)
                    assertNotNull("Expected not null har entries requests url", it.request.url)
                    assertNotNull("Expected not null har entries requests httpVersion", it.request.httpVersion)
                    assertNotNull("Expected not null har entries requests cookies", it.request.cookies)
                    assertNotNull("Expected not null har entries requests headers", it.request.headers)
                    assertNotNull("Expected not null har entries requests queryString", it.request.queryString)
                    assertNotNull("Expected not null har entries requests headersSize", it.request.headersSize)
                    assertNotNull("Expected not null har entries requests bodySize", it.request.bodySize)

                    assertNotNull("Expected not null har entries responses status", it.response.status)
                    assertNotNull("Expected not null har entries responses statusText", it.response.statusText)
                    assertNotNull("Expected not null har entries responses httpVersion", it.response.httpVersion)
                    assertNotNull("Expected not null har entries responses cookies", it.response.cookies)
                    assertNotNull("Expected not null har entries responses content", it.response.content)
                    assertNotNull("Expected not null har entries responses redirectURL", it.response.redirectURL)
                    assertNotNull("Expected not null har entries responses headersSize", it.response.headersSize)
                    assertNotNull("Expected not null har entries responses bodySize", it.response.bodySize)

                    it.response.cookies.each { cookie ->
                        assertNotNull("Expected not null har entries responses cookies name", cookie.name)
                        assertNotNull("Expected not null har entries responses cookies value", cookie.value)
                    }

                    assertNotNull("Expected not null har entries requests postData mimeType", it.request.postData.mimeType)
                    assertNotNull("Expected not null har entries requests postData params", it.request.postData.params)
                    assertNotNull("Expected not null har entries requests postData text", it.request.postData.text)

                    it.request.postData.params.each { param ->
                        assertNotNull("Expected not null har entries requests postData params name", param.name)
                    }

                    assertNotNull("Expected not null har entries responses content size", it.response.content.size)
                    assertNotNull("Expected not null har entries responses content mimeType", it.response.content.mimeType)
                    assertNotNull("Expected not null har entries responses content text", it.response.content.text)

                    assertNotNull("Expected not null har entries timings send", it.timings.send)
                    assertNotNull("Expected not null har entries timings wait", it.timings.wait)
                    assertNotNull("Expected not null har entries timings receive", it.timings.receive)
                }
            }
        }

        verify(1, getRequestedFor(urlEqualTo("/${urlToCatch}")))
    }

    protected void mockTargetServerResponse(String url, String responseBody) {
        def response = aResponse().withStatus(200)
                .withBody(responseBody)
                .withHeader('Content-Type', '')
        stubFor(get(urlEqualTo("/${url}")).willReturn(response))
    }
}
