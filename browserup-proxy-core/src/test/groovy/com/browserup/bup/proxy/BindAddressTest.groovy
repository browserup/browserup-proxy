/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.HttpHostConnectException
import org.junit.After
import org.junit.Before
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.assertEquals
import static org.junit.Assume.assumeNoException

@org.junit.Ignore
class BindAddressTest extends MockServerTest {
    private BrowserUpProxy proxy

    @Before
    void setUp() {
    }

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testClientBindAddress() {
        def stubUrl = "/clientbind"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        // bind to loopback. ProxyServerTest.getNewHtpClient creates an HTTP client that connects to a proxy at 127.0.0.1
        proxy = new BrowserUpProxyServer()
        proxy.start(0, InetAddress.getLoopbackAddress())

        NewProxyServerTestUtil.getNewHttpClient(proxy.getPort()).withCloseable {
            def response = it.execute(new HttpGet("http://127.0.0.1:${mockServerPort}/clientbind"))
            assertEquals(200, response.statusLine.statusCode)
        }
    }

    @Test(expected = HttpHostConnectException.class)
    void testClientBindAddressCannotConnect() {
        def stubUrl = "/clientbind"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        // find the local host address to bind to that isn't loopback. since ProxyServerTest.getNewHtpClient creates an HTTP client that
        // connects to a proxy at 127.0.0.1, the HTTP client should *not* be able to connect to the proxy
        InetAddress localHostAddr
        try {
            localHostAddr = InetAddress.getLocalHost()
        } catch (UnknownHostException e) {
            assumeNoException("Could not get a localhost address. Skipping test.", e)
            return
        }

        proxy = new BrowserUpProxyServer()
        proxy.start(0, localHostAddr)

        NewProxyServerTestUtil.getNewHttpClient(proxy.getPort()).withCloseable {
            it.execute(new HttpGet("http://127.0.0.1:${mockServerPort}/clientbind"))
        }
    }

    @Test
    void testServerBindAddress() {
        def stubUrl = "/serverbind"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        // bind outgoing traffic to loopback. since the mockserver is running on localhost with a wildcard address, this should succeed.
        proxy = new BrowserUpProxyServer()
        proxy.start(0, null, InetAddress.getLoopbackAddress())

        NewProxyServerTestUtil.getNewHttpClient(proxy.getPort()).withCloseable {
            def response = it.execute(new HttpGet("http://127.0.0.1:${mockServerPort}/serverbind"))
            assertEquals(200, response.statusLine.statusCode)
        }
    }

    @Test
    void testServerBindAddressCannotConnect() {
        // bind outgoing traffic to loopback. since loopback cannot reach external addresses, this should fail.
        proxy = new BrowserUpProxyServer()
        proxy.start(0, null, InetAddress.getLoopbackAddress())

        NewProxyServerTestUtil.getNewHttpClient(proxy.getPort()).withCloseable {
            def response = it.execute(new HttpGet("http://www.google.com"))
            assertEquals("Expected a 502 Bad Gateway when connecting to an external address after binding to loopback", 502, response.statusLine.statusCode)
        }
    }
}
