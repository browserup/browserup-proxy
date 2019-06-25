package com.browserup.bup.proxy.rest

import com.browserup.harreader.model.HarEntry
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

class FindHarEntriesRestTest extends BaseRestTest {

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
    }
}
