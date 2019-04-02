/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat

class FindMostRecentEntryRestTest extends BaseRestTest {
    private static final int MILLISECONDS_BETWEEN_REQUESTS = 100

    def urlOfMostRecentRequest = 'url-most-recent'
    def urlOfOldRequest = 'url-old'
    def urlPatternToMatchUrl = '.*url-.*'
    def urlPatternNotToMatchUrl = '.*does_not_match-.*'
    def responseBody = 'success'

    @Override
    String getUrlPath() {
        return 'har/mostRecentEntry'
    }

    @Test
    void findMostRecentHarEntryByUrlPattern() {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody)
        mockTargetServerResponse(urlOfOldRequest, responseBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlOfOldRequest, responseBody)

        sleep MILLISECONDS_BETWEEN_REQUESTS

        requestToTargetServer(urlOfMostRecentRequest, responseBody)

        def allCapturedEntries = [] as HarEntry[]
        proxyRestServerClient.request(Method.GET, ContentType.APPLICATION_JSON) { req ->
            uri.path = "/proxy/${proxy.port}/har/entries"
            uri.query = [urlPattern: urlPatternToMatchUrl]
            response.success = { HttpResponseDecorator resp ->
                allCapturedEntries = (new ObjectMapper().readValue(resp.entity.content, HarEntry[]) as HarEntry[])
                assertThat('Expected to find both entries', allCapturedEntries, Matchers.arrayWithSize(2))
            }
        }

        proxyRestServerClient.request(Method.GET, ContentType.APPLICATION_JSON) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern]
            response.success = { HttpResponseDecorator resp ->
                def actualEntry = new ObjectMapper().readValue(resp.entity.content, HarEntry) as HarEntry
                def expectedMostRecentEntry = allCapturedEntries.max {it.startedDateTime}
                assertNotNull('Expected to find an entry', actualEntry)
                assertThat('Expected to find most recent entry containing url from url filter pattern',
                        actualEntry.request.url, Matchers.containsString(urlOfMostRecentRequest))
                assertThat('Expected that found entry has maximum started date time',
                        actualEntry.startedDateTime, Matchers.equalTo(expectedMostRecentEntry.startedDateTime))
            }
        }

        WireMock.verify(1, getRequestedFor(urlEqualTo("/${urlOfMostRecentRequest}")))
        WireMock.verify(1, getRequestedFor(urlEqualTo("/${urlOfOldRequest}")))
    }

    @Test
    void getEmptyEntryIfNoEntryFoundByUrlPattern() {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody)
        mockTargetServerResponse(urlOfOldRequest, responseBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlOfOldRequest, responseBody)

        sleep MILLISECONDS_BETWEEN_REQUESTS

        requestToTargetServer(urlOfMostRecentRequest, responseBody)

        proxyRestServerClient.request(Method.GET, ContentType.APPLICATION_JSON) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPatternNotToMatchUrl]
            response.success = { HttpResponseDecorator resp ->
                def actualEntry = new ObjectMapper().readValue(resp.entity.content, HarEntry) as HarEntry
                assertNull('Expected to find empty entry', actualEntry.startedDateTime)
            }
        }

        WireMock.verify(1, getRequestedFor(urlEqualTo("/${urlOfMostRecentRequest}")))
        WireMock.verify(1, getRequestedFor(urlEqualTo("/${urlOfOldRequest}")))
    }
}
