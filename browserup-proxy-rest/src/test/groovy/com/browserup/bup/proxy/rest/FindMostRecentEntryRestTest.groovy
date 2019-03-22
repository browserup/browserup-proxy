package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

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
        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/har/entries"
            uri.query = [urlPattern: urlPatternToMatchUrl]
            response.success = { _, reader ->
                allCapturedEntries = (new ObjectMapper().readValue(reader, HarEntry[]) as HarEntry[])
                assertThat('Expected to find both entries', allCapturedEntries, Matchers.arrayWithSize(2))
            }
        }

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            def urlPattern = ".*${urlPatternToMatchUrl}"
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPattern]
            response.success = { _, reader ->
                def actualEntry = new ObjectMapper().readValue(reader, HarEntry) as HarEntry
                def expectedMostRecentEntry = allCapturedEntries.max {it.startedDateTime}
                assertNotNull('Expected to find an entry', actualEntry)
                assertThat('Expected to find most recent entry containing url from url filter pattern',
                        actualEntry.request.url, Matchers.containsString(urlOfMostRecentRequest))
                assertThat('Expected that found entry has maximum started date time',
                        actualEntry.startedDateTime, Matchers.equalTo(expectedMostRecentEntry.startedDateTime))
            }
        }
    }

    @Test
    void getEmptyEntryIfNoEntryFoundByUrlPattern() {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody)
        mockTargetServerResponse(urlOfOldRequest, responseBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlOfOldRequest, responseBody)

        sleep MILLISECONDS_BETWEEN_REQUESTS

        requestToTargetServer(urlOfMostRecentRequest, responseBody)

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/proxy/${proxy.port}/${urlPath}"
            uri.query = [urlPattern: urlPatternNotToMatchUrl]
            response.success = { _, reader ->
                def actualEntry = new ObjectMapper().readValue(reader, HarEntry) as HarEntry
                assertNull('Expected to find empty entry', actualEntry.startedDateTime)
            }
        }
    }
}
