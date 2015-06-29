package net.lightbody.bmp.util

import org.junit.Test

import static org.junit.Assert.assertEquals

class BrowserMobHttpUtilTest {
    @Test
    void testGetResourceFromUri() {
        Map<String, String> uriToResource = [
                'http://www.example.com/the/resource': '/the/resource',
                'https://example/resource': '/resource',
                'http://127.0.0.1/ip/address/resource': '/ip/address/resource',
                'https://hostname:8080/host/and/port/resource': '/host/and/port/resource',
                'http://hostname/': '/',
                'https://127.0.0.1/': '/',
                'http://127.0.0.1:1950/ip/port/resource': '/ip/port/resource',
                'https://[abcd:1234::17]/ipv6/literal/resource': '/ipv6/literal/resource',
                'http://[abcd:1234::17]:50/ipv6/with/port/literal/resource': '/ipv6/with/port/literal/resource',
                'https://hostname/query/param/resource?param=value': '/query/param/resource?param=value',
        ]

        uriToResource.each {uri, expectedResource ->
            String parsedResource = BrowserMobHttpUtil.getPathFromUri(uri)
            assertEquals("Parsed resource from URL did not match expected resource", expectedResource, parsedResource)
        }
    }

    @Test
    void testGetHostAndPortFromUri() {
        Map<String, String> uriToHostAndPort = [
                'http://www.example.com/some/resource': 'www.example.com',
                'https://www.example.com:8080/some/resource': 'www.example.com:8080',
                'http://127.0.0.1/some/resource': '127.0.0.1',
                'https://127.0.0.1:8080/some/resource?param=value': '127.0.0.1:8080',
                'http://localhost/some/resource': 'localhost',
                'https://localhost:1820/': 'localhost:1820',
                'http://[abcd:1234::17]/ipv6/literal/resource': '[abcd:1234::17]',
                'https://[abcd:1234::17]:50/ipv6/with/port/literal/resource': '[abcd:1234::17]:50',
        ]

        uriToHostAndPort.each {uri, expectedHostAndPort ->
            String parsedHostAndPort = BrowserMobHttpUtil.getHostAndPortFromUri(uri)
            assertEquals("Parsed host and port from URL did not match expected host and port", expectedHostAndPort, parsedHostAndPort)
        }
    }
}
