package net.lightbody.bmp.proxy

import com.google.sitebricks.headless.Request
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import net.lightbody.bmp.BrowserMobProxyServerLegacyAdapter
import net.lightbody.bmp.filters.RequestFilter
import net.lightbody.bmp.filters.ResponseFilter
import net.lightbody.bmp.proxy.bricks.ProxyResource
import net.lightbody.bmp.proxy.test.util.ProxyResourceTest
import org.apache.http.entity.ContentType
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.greaterThanOrEqualTo
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail
import static org.junit.Assume.assumeThat
import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class FilterTest extends ProxyResourceTest {
    @Test
    void testCanModifyRequestHeadersWithJavascript() {
        final String requestFilterJavaScript =
                '''
                request.headers().remove('User-Agent');
                request.headers().add('User-Agent', 'My-Custom-User-Agent-String 1.0');
                '''

        Request<String> mockRestRequest = createMockRestRequestWithEntity(requestFilterJavaScript)

        proxyResource.addRequestFilter(proxyPort, mockRestRequest)

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/modifyuseragent")
                .withHeader(new Header("User-Agent", "My-Custom-User-Agent-String 1.0")),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header("Content-Type", "text/plain"))
                .withBody("success"));

        HTTPBuilder http = getHttpBuilder()

        http.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/modifyuseragent"

            response.success = { resp, reader ->
                assertEquals("Javascript interceptor did not modify the user agent string", "success", reader.text)
            }
        }
    }


    @Test
    void testCanModifyRequestContentsWithJavascript() {
        final String requestFilterJavaScript =
                '''
                if (request.getUri().endsWith('/modifyrequest') && contents.isText()) {
                    // using == instead of === since under Java 7 the js engine treats js strings as 'string' but Java Strings as 'object'
                    if (contents.getTextContents() == 'original request text') {
                        contents.setTextContents('modified request text');
                    }
                }
                '''

        Request<String> mockRestRequest = createMockRestRequestWithEntity(requestFilterJavaScript)

        proxyResource.addRequestFilter(proxyPort, mockRestRequest)

        mockServer.when(request()
                .withMethod("PUT")
                .withPath("/modifyrequest")
                .withBody("modified request text"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header("Content-Type", "text/plain; charset=UTF-8"))
                .withBody("success"));

        HTTPBuilder http = getHttpBuilder()

        http.request(Method.PUT, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/modifyrequest"
            body = "original request text"

            response.success = { resp, reader ->
                assertEquals("Javascript interceptor did not modify request body", "success", reader.text)
            }
        }
    }

    @Test
    void testCanModifyResponseWithJavascript() {
        final String responseFilterJavaScript =
                '''
                if (contents.isText()) {
                    // using == instead of === since under Java 7 the js engine treats js strings as 'string' but Java Strings as 'object'
                    if (contents.getTextContents() == 'original response text') {
                        contents.setTextContents('modified response text');
                    }
                }
                '''

        Request<String> mockRestRequest = createMockRestRequestWithEntity(responseFilterJavaScript)

        proxyResource.addResponseFilter(proxyPort, mockRestRequest)

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/modifyresponse"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header("Content-Type", "text/plain; charset=UTF-8"))
                .withBody("original response text"));

        HTTPBuilder http = getHttpBuilder()

        http.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/modifyresponse"

            response.success = { resp, reader ->
                assertEquals("Javascript interceptor did not modify response text", "modified response text", reader.text)
            }
        }
    }

    @Test
    void testCanAccessOriginalRequestWithJavascript() {
        final String requestFilterJavaScript =
                '''
                if (request.getUri().endsWith('/originalrequest')) {
                    request.setUri(request.getUri().replaceAll('originalrequest', 'modifiedrequest'));
                }
                '''

        Request<String> mockRestAddReqFilterRequest = createMockRestRequestWithEntity(requestFilterJavaScript)
        proxyResource.addRequestFilter(proxyPort, mockRestAddReqFilterRequest)

        final String responseFilterJavaScript =
                '''
                contents.setTextContents(messageInfo.getOriginalRequest().getUri());
                '''
        Request<String> mockRestAddRespFilterRequest = createMockRestRequestWithEntity(responseFilterJavaScript)
        proxyResource.addResponseFilter(proxyPort, mockRestAddRespFilterRequest)

        mockServer.when(request()
                .withMethod("GET")
                .withPath("/modifiedrequest"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(200)
                .withHeader(new Header("Content-Type", "text/plain; charset=UTF-8"))
                .withBody("should-be-replaced"));

        HTTPBuilder http = getHttpBuilder()

        http.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/originalrequest"

            response.success = { resp, reader ->
                assertThat("Javascript interceptor did not read messageData.originalRequest variable successfully", reader.text, endsWith("originalrequest"))
            }
        }
    }

    @Test
    void testRequestFilterNotAddedIfJavascriptDoesNotCompile() {
        final String requestFilterJavaScript =
                '''
                this javascript won't compile!
                '''

        Request<String> mockRestAddReqFilterRequest = createMockRestRequestWithEntity(requestFilterJavaScript)

        // mock the proxy so we can verify the addRequestFilter() method is never called
        def mockProxy = mock(BrowserMobProxyServerLegacyAdapter)

        // mock the ProxyManager to return the mocked proxy
        ProxyManager mockProxyManager = mock(ProxyManager)
        when(mockProxyManager.get(proxyPort)).thenReturn(mockProxy)

        // not using the local ProxyResource, since we need to mock out the dependencies
        ProxyResource proxyResource = new ProxyResource(mockProxyManager)

        boolean javascriptExceptionOccurred = false;

        try {
            proxyResource.addRequestFilter(proxyPort, mockRestAddReqFilterRequest)
        } catch (JavascriptCompilationException) {
            javascriptExceptionOccurred = true;
        }

        assertTrue("Expected javascript to fail to compile", javascriptExceptionOccurred)

        verify(mockProxy, never()).addRequestFilter(any(RequestFilter))
    }

    @Test
    void testResponseFilterNotAddedIfJavascriptDoesNotCompile() {
        final String responseFilterJavaScript =
                '''
                this javascript won't compile!
                '''

        Request<String> mockRestAddRespFilterRequest = createMockRestRequestWithEntity(responseFilterJavaScript)

        // mock the proxy so we can verify the addResponseFilter() method is never called
        def mockProxy = mock(BrowserMobProxyServerLegacyAdapter)

        // mock the ProxyManager to return the mocked proxy
        ProxyManager mockProxyManager = mock(ProxyManager)
        when(mockProxyManager.get(proxyPort)).thenReturn(mockProxy)

        // not using the local ProxyResource, since we need to mock out the dependencies
        ProxyResource proxyResource = new ProxyResource(mockProxyManager)

        boolean javascriptExceptionOccurred = false;

        try {
            proxyResource.addResponseFilter(proxyPort, mockRestAddRespFilterRequest)
        } catch (JavascriptCompilationException) {
            javascriptExceptionOccurred = true;
        }

        assertTrue("Expected javascript to fail to compile", javascriptExceptionOccurred)

        verify(mockProxy, never()).addResponseFilter(any(ResponseFilter))
    }

    @Test
    void testCanShortCircuitRequestWithJavascript() {
        def javaVersion = System.getProperty("java.specification.version") as double
        assumeThat("Skipping Nashorn-dependent test on Java 1.7", javaVersion, greaterThanOrEqualTo(1.8d))

        final String requestFilterJavaScript =
                '''
                // "import" classes
                var DefaultFullHttpResponse = Java.type('io.netty.handler.codec.http.DefaultFullHttpResponse');
                var HttpResponseStatus = Java.type('io.netty.handler.codec.http.HttpResponseStatus');
                var HttpObjectUtil = Java.type('net.lightbody.bmp.util.HttpObjectUtil');

                // create a new DefaultFullHttpResponse that will short-circuit the request
                var shortCircuitRequest = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.PAYMENT_REQUIRED);

                // use the convenient HttpObjectUtil.replaceTextHttpEntityBody() method to set the entity body
                var responseBody = 'You have to pay the troll toll to get into this Proxy\\'s soul';
                HttpObjectUtil.replaceTextHttpEntityBody(shortCircuitRequest, responseBody);

                // return the short-circuit FullHttpResponse
                shortCircuitRequest;
                '''

        Request<String> mockRestRequest = createMockRestRequestWithEntity(requestFilterJavaScript)

        proxyResource.addRequestFilter(proxyPort, mockRestRequest)

        HTTPBuilder http = getHttpBuilder()

        http.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/testShortCircuit"

            response.success = { resp, reader ->
                fail("Expected short-circuit response to return an HTTP 402 Payment Required")
            }

            response.failure = { resp, reader ->
                assertEquals("Expected short-circuit response to return an HTTP 402 Payment Required", 402, resp.status)
                assertEquals("Expected short-circuit response to contain body text set in Javascript", "You have to pay the troll toll to get into this Proxy's soul", reader.text)
            }
        }
    }

    @Override
    String[] getArgs() {
        return ["--use-littleproxy", "true"]
    }
}
