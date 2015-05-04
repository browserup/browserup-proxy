package net.lightbody.bmp.proxy

import com.google.sitebricks.headless.Request
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import net.lightbody.bmp.BrowserMobProxyServer
import net.lightbody.bmp.filters.RequestFilter
import net.lightbody.bmp.filters.ResponseFilter
import net.lightbody.bmp.proxy.bricks.ProxyResource
import net.lightbody.bmp.proxy.test.util.ProxyResourceTest
import org.apache.http.entity.ContentType
import org.junit.Test
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.hamcrest.Matchers.endsWith
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
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
                contents.setTextContents(originalRequest.getUri());
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
                assertThat("Javascript interceptor did not read originalRequest variable successfully", reader.text, endsWith("originalrequest"))
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
        BrowserMobProxyServer mockProxy = mock(BrowserMobProxyServer)

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
        BrowserMobProxyServer mockProxy = mock(BrowserMobProxyServer)

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

    @Override
    String[] getArgs() {
        return ["--use-littleproxy", "true"]
    }
}
