package net.lightbody.bmp.proxy

import com.google.common.collect.Iterables
import net.lightbody.bmp.BrowserMobProxy
import net.lightbody.bmp.BrowserMobProxyServer
import net.lightbody.bmp.core.har.Har
import net.lightbody.bmp.core.har.HarContent
import net.lightbody.bmp.core.har.HarCookie
import net.lightbody.bmp.core.har.HarEntry
import net.lightbody.bmp.core.har.HarNameValuePair
import net.lightbody.bmp.proxy.dns.AdvancedHostResolver
import net.lightbody.bmp.proxy.dns.BasicHostResolver
import net.lightbody.bmp.proxy.test.util.MockServerTest
import net.lightbody.bmp.proxy.test.util.ProxyServerTest
import net.lightbody.bmp.proxy.util.IOUtils
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockserver.matchers.Times
import org.mockserver.model.Cookie
import org.mockserver.model.Header

import java.util.concurrent.TimeUnit

import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.greaterThan
import static org.hamcrest.Matchers.greaterThanOrEqualTo
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

/**
 * HAR tests using the new interface. When the legacy interface is retired, these tests should be combined with the tests currently in HarTest.
 */
class NewHarTest extends MockServerTest {
    private BrowserMobProxy proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testDnsTimingPopulated() {
        // mock up a resolver with a DNS resolution delay
        AdvancedHostResolver mockResolver = mock(AdvancedHostResolver.class);
        when(mockResolver.resolve("localhost")).then(new Answer<Collection<InetAddress>>() {
            @Override
            public Collection<InetAddress> answer(InvocationOnMock invocationOnMock) throws Throwable {
                TimeUnit.SECONDS.sleep(1);
                return Collections.singleton(InetAddress.getByName("localhost"));
            }
        });

        // mock up a response to serve
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/testDnsTimingPopulated"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withBody("success"));

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setHostNameResolver(mockResolver);

        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.newHar();

        ProxyServerTest.getNewHttpClient(proxyPort).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testDnsTimingPopulated")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar();

        assertNotNull("HAR should not be null", har);
        assertNotNull("HAR log should not be null", har.getLog());
        assertNotNull("HAR log entries should not be null", har.getLog().getEntries());
        assertFalse("HAR entries should exist", har.getLog().getEntries().isEmpty());

        HarEntry entry = Iterables.get(har.getLog().getEntries(), 0);
        assertThat("Expected at least 1 second DNS delay", entry.getTimings().getDns(), greaterThanOrEqualTo(1000L));
    }

    @Test
    void testCaptureResponseCookiesInHar() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/testCaptureResponseCookiesInHar"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withBody("success")
                .withCookie(new Cookie("mock-cookie", "mock value")))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_COOKIES] as Set)
        proxy.start()

        proxy.newHar()

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testCaptureResponseCookiesInHar")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))
        assertThat("Expected to find cookies in the HAR", har.getLog().getEntries().first().response.cookies, not(empty()))

        HarCookie cookie = har.getLog().getEntries().first().response.cookies.first()
        assertEquals("Incorrect cookie name in HAR", "mock-cookie", cookie.name)
        assertEquals("Incorrect cookie value in HAR", "mock value", cookie.value)
    }

    @Test
    void testCaptureResponseHeaderInHar() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/testCaptureResponseHeaderInHar"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withBody("success")
                .withHeader(new Header("Mock-Header", "mock value")))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_HEADERS] as Set)
        proxy.start()

        proxy.newHar()

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testCaptureResponseHeaderInHar")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        List<HarNameValuePair> headers = har.getLog().getEntries().first().response.headers
        assertThat("Expected to find headers in the HAR", headers, not(empty()))

        HarNameValuePair header = headers.find { it.name == "Mock-Header" }
        assertNotNull("Expected to find header with name Mock-Header in HAR", header)
        assertEquals("Incorrect header value for Mock-Header", "mock value", header.value)
    }

    @Test
    void testCaptureResponseContentInHar() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/testCaptureResponseContentInHar"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withBody("success")
                .withHeader(new Header("Content-Type", "text/plain; charset=UTF-8")))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_CONTENT] as Set)
        proxy.start()

        proxy.newHar()

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testCaptureResponseContentInHar")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        HarContent content = har.getLog().getEntries().first().response.content
        assertNotNull("Expected to find HAR content", content)

        assertEquals("Expected to capture body content in HAR", "success", content.text)
    }

    @Test
    void testEndHar() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/testEndHar"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(200)
                .withBody("success")
                .withHeader(new Header("Content-Type", "text/plain; charset=UTF-8")))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_CONTENT] as Set)
        proxy.start()

        proxy.newHar()

        // putting tests in code blocks to avoid variable name collisions
        regularHarCanCapture: {
            ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "success", responseBody);
            };

            Thread.sleep(500)
            Har har = proxy.endHar()

            assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

            HarContent content = har.getLog().getEntries().first().response.content
            assertNotNull("Expected to find HAR content", content)

            assertEquals("Expected to capture body content in HAR", "success", content.text)

            assertThat("Expected HAR page timing onLoad value to be populated", har.log.pages.last().pageTimings.onLoad, greaterThan(0L))
        }

        harEmptyAfterEnd: {
            Har emptyHar = proxy.getHar()

            assertNull("Expected getHar() to return null after calling endHar()", emptyHar)
        }

        harStillEmptyAfterRequest: {
            ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "success", responseBody);
            };

            Thread.sleep(500)
            Har stillEmptyHar = proxy.getHar()

            assertNull("Expected getHar() to return null after calling endHar()", stillEmptyHar)
        }

        newHarInitiallyEmpty: {
            Har newHar = proxy.newHar()

            assertNull("Expected newHar() to return the old (null) har", newHar)
        }

        newHarCanCapture: {
            ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "success", responseBody);
            };

            Thread.sleep(500)
            Har populatedHar = proxy.getHar()

            assertThat("Expected to find entries in the HAR", populatedHar.getLog().getEntries(), not(empty()))

            HarContent newContent = populatedHar.getLog().getEntries().first().response.content
            assertNotNull("Expected to find HAR content", newContent)

            assertEquals("Expected to capture body content in HAR", "success", newContent.text)
        }
    }

    @Test
    void testNewPageReturnsHarInPreviousState() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/testEndHar"),
                Times.unlimited())
                .respond(response()
                .withStatusCode(200)
                .withBody("success")
                .withHeader(new Header("Content-Type", "text/plain; charset=UTF-8")))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_CONTENT] as Set)
        proxy.start()

        proxy.newHar("first-page")

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        HarContent content = har.getLog().getEntries().first().response.content
        assertNotNull("Expected to find HAR content", content)

        assertEquals("Expected to capture body content in HAR", "success", content.text)

        assertEquals("Expected only one HAR page to be created", 1, har.log.pages.size())
        assertEquals("Expected id of HAR page to be 'first-page'", "first-page", har.log.pages.first().id)

        Har harWithFirstPageOnly = proxy.newPage("second-page")

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har harWithSecondPage = proxy.getHar()

        assertEquals("Expected HAR to contain first and second page page", 2, harWithSecondPage.log.pages.size())
        assertEquals("Expected id of second HAR page to be 'second-page'", "second-page", harWithSecondPage.log.pages[1].id)

        assertEquals("Expected HAR returned from newPage() not to contain second page", 1, harWithFirstPageOnly.log.pages.size())
        assertEquals("Expected id of HAR page to be 'first-page'", "first-page", harWithFirstPageOnly.log.pages.first().id)
    }

    @Test
    void testCaptureHttpRequestUrlInHar() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/httprequesturlcaptured"),
                Times.once())
                .respond(response()
                .withStatusCode(200)
                .withBody("success"))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.start()

        proxy.newHar()

        String requestUrl = "http://localhost:${mockServerPort}/httprequesturlcaptured"

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet(requestUrl)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)
    }

    @Test
    void testCaptureHttpRequestUrlWithQueryParamInHar() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/httprequesturlcaptured")
                .withQueryStringParameter("param1", "value1"),
                Times.once())
                .respond(response()
                .withStatusCode(200)
                .withBody("success"))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.start()

        proxy.newHar()

        String requestUrl = "http://localhost:${mockServerPort}/httprequesturlcaptured?param1=value1"

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet(requestUrl)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

        assertThat("Expected to find query parameters in the HAR", har.log.entries[0].request.queryString, not(empty()));

        assertEquals("Expected first query parameter name to be param1", "param1", har.log.entries[0].request.queryString[0].name)
        assertEquals("Expected first query parameter value to be value1", "value1", har.log.entries[0].request.queryString[0].value)
    }

    @Test
    void testCaptureHttpsRequestUrlInHar() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/httpsrequesturlcaptured")
                .withQueryStringParameter("param1", "value1"),
                Times.once())
                .respond(response()
                .withStatusCode(200)
                .withBody("success"))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.start()

        proxy.newHar()

        // use HTTPS to force a CONNECT. subsequent requests through the tunnel will only contain te resource path, not the full hostname.
        String requestUrl = "https://localhost:${mockServerPort}/httpsrequesturlcaptured?param1=value1"

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet(requestUrl)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

        assertThat("Expected to find query parameters in the HAR", har.log.entries[0].request.queryString, not(empty()));

        assertEquals("Expected first query parameter name to be param1", "param1", har.log.entries[0].request.queryString[0].name)
        assertEquals("Expected first query parameter value to be value1", "value1", har.log.entries[0].request.queryString[0].value)
    }

    @Test
    void testCaptureHttpsRewrittenUrlInHar() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/httpsrewrittenurlcaptured")
                .withQueryStringParameter("param1", "value1"),
                Times.once())
                .respond(response()
                .withStatusCode(200)
                .withBody("success"))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.rewriteUrl("www.rewrittenurl.com:443", "localhost:${mockServerPort}")
        proxy.start()

        proxy.newHar()

        String requestUrl = "https://www.rewrittenurl.com/httpsrewrittenurlcaptured?param1=value1"
        String rewrittenUrl = "https://localhost:${mockServerPort}/httpsrewrittenurlcaptured?param1=value1"

        ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet(requestUrl)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        };

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", rewrittenUrl, capturedUrl)

        assertThat("Expected to find query parameters in the HAR", har.log.entries[0].request.queryString, not(empty()));

        assertEquals("Expected first query parameter name to be param1", "param1", har.log.entries[0].request.queryString[0].name)
        assertEquals("Expected first query parameter value to be value1", "value1", har.log.entries[0].request.queryString[0].value)
    }

    @Test
    void testMitmDisabledStopsHTTPCapture() {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/httpmitmdisabled"),
                Times.once())
                .respond(response()
                .withStatusCode(200)
                .withBody("Response over HTTP"))

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/httpsmitmdisabled"),
                Times.exactly(2))
                .respond(response()
                .withStatusCode(200)
                .withBody("Response over HTTPS"))

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setMitmDisabled(true);
        proxy.start()

        proxy.newHar()

        httpsRequest: {
            String httpsUrl = "https://localhost:${mockServerPort}/httpsmitmdisabled"
            ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet(httpsUrl)).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "Response over HTTPS", responseBody);
            };

            Thread.sleep(500)
            Har har = proxy.getHar()

            assertThat("Expected to find no entries in the HAR because MITM is disabled", har.getLog().getEntries(), empty())
        }

        httpRequest: {
            String httpUrl = "http://localhost:${mockServerPort}/httpmitmdisabled"
            ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet(httpUrl)).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "Response over HTTP", responseBody);
            };

            Thread.sleep(500)
            Har har = proxy.getHar()

            assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

            String capturedUrl = har.log.entries[0].request.url
            assertEquals("URL captured in HAR did not match request URL", httpUrl, capturedUrl)
        }

        secondHttpsRequest: {
            String httpsUrl = "https://localhost:${mockServerPort}/httpsmitmdisabled"
            ProxyServerTest.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = IOUtils.toStringAndClose(it.execute(new HttpGet(httpsUrl)).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "Response over HTTPS", responseBody);
            };

            Thread.sleep(500)
            Har har = proxy.getHar()

            assertThat("Expected to find only the HTTP entry in the HAR", har.getLog().getEntries(), hasSize(1))
        }

    }

    //TODO: Add Request Capture Type tests
}
