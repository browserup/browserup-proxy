/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com

import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

class FindHarEntriesRestTest extends WithRunningProxyRestTest {

    @Override
    String getUrlPath() {
        return 'har/entries'
    }

    @Test
    void findHarEntryByUrlPattern() {
        def urlToCatch = 'test'
        def urlNotToCatch = 'missing'
        def responseBody = 'success'

        mockTargetServerResponse(urlToCatch, responseBody)
        mockTargetServerResponse(urlNotToCatch, responseBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlToCatch, responseBody)
        requestToTargetServer(urlNotToCatch, responseBody)

        proxyRestServerClient.request(Method.GET, ContentType.APPLICATION_JSON) { req ->
            def urlPattern = ".*${urlToCatch}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern]
            response.success = { HttpResponseDecorator resp ->
                HarEntry[] entries = new ObjectMapper().readValue(resp.entity.content, HarEntry[]) as HarEntry[]
                assertThat('Expected to find only one entry', entries, Matchers.arrayWithSize(1))
                assertThat('Expected to find entry containing url from url filter pattern',
                        entries[0].request.url, Matchers.containsString(urlToCatch))
                assertThat('Expected to find no entries with urlNotToCatch filter',
                        entries[0].request.url, Matchers.not(Matchers.containsString(urlNotToCatch)))
            }
        }

        WireMock.verify(1, getRequestedFor(urlEqualTo("/${urlToCatch}")))
        WireMock.verify(1, getRequestedFor(urlEqualTo("/${urlNotToCatch}")))
    }

    @Test
    void getEmptyEntriesArrayIfNoEntriesFoundByUrl() {
        def urlToCatch = 'test'
        def urlNotToCatch = 'missing'
        def responseBody = 'success'

        mockTargetServerResponse(urlNotToCatch, responseBody)

        proxyManager.get()[0].newHar()

        targetServerClient.request(Method.GET, ContentType.WILDCARD) { req ->
            uri.path = "/${urlNotToCatch}"
            response.success = { _, reader ->
                assertEquals(responseBody, reader.text)
            }
        }

        proxyRestServerClient.request(Method.GET, ContentType.APPLICATION_JSON) { req ->
            def urlPattern = ".*${urlToCatch}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern]
            response.success = { HttpResponseDecorator resp ->
                HarEntry[] entries = new ObjectMapper().readValue(resp.entity.content, HarEntry[]) as HarEntry[]
                assertThat('Expected get empty har entries array', entries, Matchers.arrayWithSize(0))
            }
        }

        WireMock.verify(1, getRequestedFor(urlEqualTo("/${urlNotToCatch}")))
    }
}
