package com.browserup.bup

import com.browserup.bup.mitmproxy.MitmProxyProcessManager
import com.browserup.harreader.model.Har
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.junit.After
import org.junit.Assert
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue;

class MitmProxyProcessManagerTest {
    def mitmProxyManager = MitmProxyProcessManager.getInstance()

    @After
    void setUp() {
        mitmProxyManager.stop()
    }

    @Test
    void proxyStartedAndHarIsAvailable() {
        //GIVEN
        def proxyPort = 8443
        mitmProxyManager.start(proxyPort)

        //WHEN
        sendRequestThroughProxy(proxyPort)

        //THEN
        Har har = mitmProxyManager.getHar()
        Assert.assertNotNull(har)
        assertTrue("Expected to capture 1 har entry", har.log.entries.size() == 1)
    }



    @Test
    void proxyStartedAndHarCleanRequestParamCleansHar() {
        //GIVEN
        def proxyPort = 8443
        mitmProxyManager.start(proxyPort)

        //WHEN
        sendRequestThroughProxy(proxyPort)

        //THEN
        Har har = mitmProxyManager.getHar()
        Assert.assertNotNull(har)
        assertTrue("Expected to capture 1 har entry", har.log.entries.size() == 1)

        har = mitmProxyManager.getHar(true)
        Assert.assertNotNull(har)
        assertTrue("Expected to capture 1 har entry", har.log.entries.size() == 1)

        har = mitmProxyManager.getHar()
        Assert.assertNotNull(har)
        assertTrue("Expected to capture no har entries", har.log.entries.size() == 0)
    }

    @Test
    void proxyStartedCurrentHarIsBeingPopulated() {
        //GIVEN
        def proxyPort = 8443
        mitmProxyManager.start(proxyPort)

        //WHEN
        def reqNumber = 5
        (1..reqNumber).each {
            sendRequestThroughProxy(proxyPort)
        }

        //THEN
        Har har = mitmProxyManager.getHar()
        Assert.assertNotNull(har)
        assertEquals("Expected to capture $reqNumber har entries", reqNumber, har.log.entries.size())

        // One more request through proxy
        sendRequestThroughProxy(proxyPort)

        har = mitmProxyManager.getHar()
        Assert.assertNotNull(har)
        assertTrue("Expected to capture 1 har entry", har.log.entries.size() == reqNumber + 1)
    }

    @Test
    void afterStopLastCapturedHarIsReturned() {
        //GIVEN
        def proxyPort = 8443
        mitmProxyManager.start(proxyPort)

        //WHEN
        sendRequestThroughProxy(proxyPort)

        mitmProxyManager.stop()
        http = new HTTPBuilder("http://petclinic.targets.browserup.com/")
        http.setProxy("localhost", proxyPort, "http")
        def ex = null
        try {
            http.request(Method.GET) {
                uri.path = '/'
            }
        } catch (e) {
            ex = e
        }
        Assert.assertNotNull(ex)
        assertTrue(ex.message.contains('Connection refused'))

        def har = mitmProxyManager.getHar()
        Assert.assertNotNull(har)
        assertTrue("Expected to capture 1 har entry", har.log.entries.size() == 1)
    }


    @Test
    void afterStopConnectionToProxyRefused() {
        //GIVEN
        def proxyPort = 8443
        mitmProxyManager.start(proxyPort)

        //WHEN
        sendRequestThroughProxy(proxyPort)

        mitmProxyManager.stop()
        http = new HTTPBuilder("http://petclinic.targets.browserup.com/")
        http.setProxy("localhost", proxyPort, "http")
        def ex = null
        try {
            http.request(Method.GET) {
                uri.path = '/'
            }
        } catch (e) {
            ex = e
        }
        Assert.assertNotNull(ex)
        assertTrue(ex.message.contains('Connection refused'))
    }

    private void sendRequestThroughProxy(int proxyPort) {
        def http = new HTTPBuilder("http://petclinic.targets.browserup.com/")
        http.setProxy("localhost", proxyPort, "http");
        http.request(Method.GET) {
            uri.path = '/'
            response.failure = { resp, json ->
                throw new AssertionError("Expected to get successful response, got: ${resp.status}")
            }
        }
    }
}