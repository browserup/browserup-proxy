/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitmproxy

import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.harreader.model.HarRequest
import com.browserup.harreader.model.HarResponse
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.awaitility.Awaitility
import org.junit.After
import org.junit.Test

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.getNewHttpClient
import static com.github.tomakehurst.wiremock.client.WireMock.*
import static java.util.concurrent.Executors.newSingleThreadExecutor
import static org.awaitility.Awaitility.await
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals
/**
 * These tests use mocked server with long delay to verify behavior of 'unbalanced' har entries, where there might be request-only, response-only or HAR entry with both request and response, depending on when 'clean har' is called during request/response/reporting process.
 */
class UnbalancedHarEntriesTest extends MockServerTest {
    private static final def DEFAULT_HAR_RESPONSE = new HarResponse()
    private static final def DEFAULT_HAR_REQUEST = new HarRequest()

    private MitmProxyServer proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testResponseOnlyHarEntryReceivedIfNoResponseYet() {
        //GIVEN
        def stubUrl = "/testResponseTimeoutCapturedInHar"
        def targetServerDelaySec = 5
        def targetServiceResponseCode = 200
        def idleConnectionTimeout = targetServerDelaySec + 1

        configureMockServer(stubUrl, targetServerDelaySec, targetServiceResponseCode)

        proxy = startProxyAndCreateHar(idleConnectionTimeout)

        def requestUrl = "http://localhost:${mockServerPort}$stubUrl".toString()

        //WHEN
        sendRequestToMockServer(requestUrl, targetServiceResponseCode)

        // Let request to be sent and captured
        sleep(500)

        def har = proxy.getHar()

        //THEN
        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, har.log.entries.first().response)
    }

    @Test
    void testGetRequestOnlyEntryAndVerifyPopulatedEntry() {
        //GIVEN
        def stubUrl = "/testResponseTimeoutCapturedInHar"
        def targetServerDelaySec = 5
        def targetServiceResponseCode = 200
        def idleConnectionTimeout = targetServerDelaySec + 1

        configureMockServer(stubUrl, targetServerDelaySec, 200)

        proxy = startProxyAndCreateHar(idleConnectionTimeout)

        def requestUrl = "http://localhost:${mockServerPort}$stubUrl".toString()

        //WHEN
        def responseReceived = sendRequestToMockServer(requestUrl, targetServiceResponseCode)

        // Let request to be sent and captured
        sleep(500)

        //THEN
        // Verify we got request-only har entry
        def har = proxy.getHar()
        def harEntry = har.log.entries.first()
        def capturedUrl = harEntry.request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, harEntry.response)

        // Wait until response it received
        await().atMost(targetServerDelaySec + 1, TimeUnit.SECONDS).until({ responseReceived.get() })

        // Verify we got response-only har entry
        har = proxy.getHar()
        harEntry = har.log.entries.first()
        assertNotEquals("Expected request to be not default", DEFAULT_HAR_REQUEST, harEntry.request)
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)
        assertNotEquals("Expected response to be not defualt", DEFAULT_HAR_RESPONSE, harEntry.response)
        assertEquals("Expected response status to be $targetServiceResponseCode",
                harEntry.response.status, targetServiceResponseCode)
        assertNotEquals("Expected response http version to be populated",
                harEntry.response.httpVersion, DEFAULT_HAR_RESPONSE.httpVersion)
    }

    @Test
    void testMultipleRequestsAndForSlowOneWeGetOnlyResponseOnlyEntry() {
        //GIVEN
        def stubUrl = "/testResponseTimeoutCapturedInHar"
        def targetServerDelaySec = 5
        def targetServiceResponseCode = 200
        def idleConnectionTimeoutSec = targetServerDelaySec + 1

        configureMockServer(stubUrl, targetServerDelaySec, targetServiceResponseCode)

        proxy = startProxyAndCreateHar(idleConnectionTimeoutSec)

        def requestUrl = "http://localhost:${mockServerPort}$stubUrl".toString()

        def otherRequests = [
                'https://browserup.com/wp-content/themes/browserup/images/logo-text-475x93.png?1',
                'https://browserup.com/wp-content/themes/browserup/images/logo-text-475x93.png?2',
                'https://browserup.com/wp-content/themes/browserup/images/logo-text-475x93.png?3'
        ]

        //WHEN
        def responseReceived = sendRequestToMockServer(requestUrl, targetServiceResponseCode)
        def responsesReceived = new ArrayList<AtomicBoolean>()
        otherRequests.each {
            responsesReceived.add(sendRequestToMockServer(it, null))
        }
        def totalNumberOfRequests = otherRequests.size() + 1

        // Wait for 'other' requests
        await().atMost(7, TimeUnit.SECONDS).until {
            !responsesReceived.any { !it.get()}
        }

        //THEN
        // Verify we got request-only har entry for mocked server
        def har = proxy.getHar()
        assertEquals("Expected to get correct number of entries", totalNumberOfRequests, har.log.entries.size())

        def harEntryForMockedServer = har.log.entries.find { it.request.url.contains("localhost") }
        def capturedUrl = harEntryForMockedServer.request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, harEntryForMockedServer.response)

        // Wait until response it received
        await().atMost(targetServerDelaySec + 1, TimeUnit.SECONDS).until({ responseReceived.get() })

        // Verify this time har entry for mocked server contains both request and response
        har = proxy.getHar()
        assertEquals("Expected to get correct number of entries", totalNumberOfRequests, har.log.entries.size())

        harEntryForMockedServer = har.log.entries.find { it.request.url.contains("localhost") }
        assertNotEquals("Expected request to be not default", DEFAULT_HAR_REQUEST, harEntryForMockedServer.request)
        assertEquals("URL captured in HAR did not match request URL", requestUrl, harEntryForMockedServer.request.url)
        assertNotEquals("Expected response to be not defualt", DEFAULT_HAR_RESPONSE, harEntryForMockedServer.response)
        assertEquals("Got unexpected response status",
                harEntryForMockedServer.response.status, targetServiceResponseCode)
        assertNotEquals("Expected response http version to be populated",
                harEntryForMockedServer.response.httpVersion, DEFAULT_HAR_RESPONSE.httpVersion)
    }

    @Test
    void testMultipleRequestsAndAfterCleanHarWeGetOnlyOneResponseOnlyEntry() {
        //GIVEN
        def stubUrl = "/testResponseTimeoutCapturedInHar"
        def targetServerDelaySec = 5
        def targetServiceResponseCode = 200
        def idleConnectionTimeoutSec = targetServerDelaySec + 1

        configureMockServer(stubUrl, targetServerDelaySec, targetServiceResponseCode)

        proxy = startProxyAndCreateHar(idleConnectionTimeoutSec)

        def requestUrl = "http://localhost:${mockServerPort}$stubUrl".toString()

        def otherRequests = [
                'https://browserup.com/wp-content/themes/browserup/images/logo-text-475x93.png?1',
                'https://browserup.com/wp-content/themes/browserup/images/logo-text-475x93.png?2',
                'https://browserup.com/wp-content/themes/browserup/images/logo-text-475x93.png?3'
        ]

        //WHEN
        def responseReceived = sendRequestToMockServer(requestUrl, targetServiceResponseCode)
        def responsesReceived = new ArrayList<AtomicBoolean>()
        otherRequests.each {
            responsesReceived.add(sendRequestToMockServer(it, null))
        }
        def totalNumberOfRequests = otherRequests.size() + 1

        // Wait for 'other' requests
        await().atMost(7, TimeUnit.SECONDS).until {
            !responsesReceived.any { !it.get()}
        }

        //THEN
        // Verify we got request-only har entry for mocked server and clean har
        def har = proxy.getHar(true)
        assertEquals("Expected to get correct number of entries", totalNumberOfRequests, har.log.entries.size())

        def harEntryForMockedServer = har.log.entries.find { it.request.url.contains("localhost") }
        def capturedUrl = harEntryForMockedServer.request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, harEntryForMockedServer.response)

        // Wait until response it received
        await().atMost(targetServerDelaySec + 1, TimeUnit.SECONDS).until({ responseReceived.get() })

        // Verify this time har entry for mocked server contains both request and response
        har = proxy.getHar()
        assertEquals("Expected to get only one entry for slow request after clean har", 1, har.log.entries.size())

        harEntryForMockedServer = har.log.entries.first()
        assertEquals("Expected request to be default", DEFAULT_HAR_REQUEST, harEntryForMockedServer.request)
        assertNotEquals("Expected response to be not defualt", DEFAULT_HAR_RESPONSE, harEntryForMockedServer.response)
        assertEquals("Got unexpected response status",
                harEntryForMockedServer.response.status, targetServiceResponseCode)
        assertNotEquals("Expected response http version to be populated",
                harEntryForMockedServer.response.httpVersion, DEFAULT_HAR_RESPONSE.httpVersion)
    }

    @Test
    void testSlowEndpointGetRequestOnlyEntryAndCleanHarAndVerifyResponseOnlyEntry() {
        //GIVEN
        def stubUrl = "/testResponseTimeoutCapturedInHar"
        def targetServerDelaySec = 5
        def idleConnectionTimeoutSec = targetServerDelaySec + 1
        def targetServiceResponseCode = 200
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(aResponse().withStatus(targetServiceResponseCode)
                        .withFixedDelay(TimeUnit.SECONDS.toMillis(targetServerDelaySec) as Integer)
                        .withBody("success"))
        )

        proxy = startProxyAndCreateHar(idleConnectionTimeoutSec)

        def requestUrl = "http://localhost:${mockServerPort}$stubUrl".toString()

        def responseReceived = sendRequestToMockServer(requestUrl, targetServiceResponseCode)


        // Let request to be sent and captured
        sleep(500)

        // Verify we got request-only har entry
        def har = proxy.getHar()
        def harEntry = har.log.entries.first()
        def capturedUrl = harEntry.request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, harEntry.response)

        // Clean HAR
        proxy.getHar(true)

        // Wait until response it received
        await().atMost(targetServerDelaySec + 1, TimeUnit.SECONDS).until({ responseReceived.get() })

        // Verify we got response-only har entry
        har = proxy.getHar()
        harEntry = har.log.entries.first()
        assertEquals("Expected request to be default", DEFAULT_HAR_REQUEST, harEntry.request)
        assertNotEquals("Expected response to be not defualt", DEFAULT_HAR_RESPONSE, harEntry.response)
        assertEquals("Expected response status to be $targetServiceResponseCode",
                harEntry.response.status, targetServiceResponseCode)
        assertNotEquals("Expected response http version to be populated",
                harEntry.response.httpVersion, DEFAULT_HAR_RESPONSE.httpVersion)
    }

    private void configureMockServer(String url, int delaySec, int responseCode) {
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse().withStatus(responseCode)
                        .withFixedDelay(TimeUnit.SECONDS.toMillis(delaySec) as Integer)
                        .withBody("success"))
        )
    }

    private MitmProxyServer startProxyAndCreateHar(int idleConnectionTimeout) {
        proxy = new MitmProxyServer()
        proxy.setIdleConnectionTimeout(idleConnectionTimeout, TimeUnit.SECONDS)
        proxy.start()
        proxy.newHar()
        proxy
    }

    private AtomicBoolean sendRequestToMockServer(requestUrl, targetServiceResponseCode) {
        def responseReceived = new AtomicBoolean(false)
        newSingleThreadExecutor().submit {
            getNewHttpClient(proxy.port).withCloseable {
                CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
                responseReceived.set(true)
                if (targetServiceResponseCode != null) {
                    assertEquals("Did not receive HTTP $targetServiceResponseCode from proxy",
                            targetServiceResponseCode, response.getStatusLine().getStatusCode())
                }
            }
        }
        responseReceived
    }
}
