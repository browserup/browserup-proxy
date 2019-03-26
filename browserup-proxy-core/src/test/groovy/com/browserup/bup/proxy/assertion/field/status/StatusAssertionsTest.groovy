package com.browserup.bup.proxy.assertion.field.status

import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import io.netty.handler.codec.http.HttpStatusClass
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.junit.Test
import org.mockserver.matchers.Times

import java.util.regex.Pattern

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose
import static org.junit.Assert.*
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class StatusAssertionsTest extends BaseAssertionsTest {

    @Test
    void currentUrlsStatusCodesBelongToClassPass() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)
        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(HttpStatusClass.SUCCESS)

        assertTrue("Expected statuses of all responses to have the same class", result.passed)
        assertFalse("Expected statuses of all responses to have the same class", result.failed)
    }

    @Test
    void currentUrlsStatusCodesBelongToClassFail() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)
        mockResponse(URL_PATH, HttpStatus.SC_NOT_FOUND)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(HttpStatusClass.SUCCESS)

        assertFalse("Expected some response statuses to belong to other classes", result.passed)
        assertTrue("Expected some response statuses to belong to other classes" , result.failed)
    }

    @Test
    void currentUrlsStatusCodesEqualPass() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)
        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(status)

        assertTrue("Expected statuses of all responses to match", result.passed)
        assertFalse("Expected statuses of all responses to match", result.failed)
    }

    @Test
    void currentUrlsStatusCodesEqualFail() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)
        mockResponse(URL_PATH, HttpStatus.SC_NOT_FOUND)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(status)

        assertFalse("Expected some responses statuses not to match", result.passed)
        assertTrue("Expected some responses statuses not to match", result.failed)
    }

    @Test
    void mostRecentUrlStatusCodeBelongToClassPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(Pattern.compile(".*${URL_PATH}.*"), HttpStatusClass.SUCCESS)

        assertTrue("Expected most recent response status to belong to the class", result.passed)
        assertFalse("Expected most recent response status to belong to the class", result.failed)
    }

    @Test
    void mostRecentUrlStatusCodeBelongToClassFails() {
        def status = HttpStatus.SC_NOT_FOUND

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(Pattern.compile(".*${URL_PATH}.*"), HttpStatusClass.SUCCESS)

        assertFalse("Expected most recent response status not to belong to the class", result.passed)
        assertTrue("Expected most recent response status not to belong to the class", result.failed)
    }

    @Test
    void mostRecentUrlStatusCodeEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(Pattern.compile(".*${URL_PATH}.*"), status)

        assertTrue("Expected to find status", result.passed)
        assertFalse("Expected to find header value", result.failed)
    }

    @Test
    void mostRecentUrlStatusCodeEqualsFails() {
        def expectedStatus = HttpStatus.SC_OK
        def status = HttpStatus.SC_BAD_REQUEST

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(Pattern.compile(".*${URL_PATH}.*"), expectedStatus)

        assertFalse("Expected not to find status", result.passed)
        assertTrue("Expected not to find header value", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusBelongsToClassPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(Pattern.compile(".*will_not_match.*"), HttpStatusClass.SUCCESS)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusDoesNotBelongToClassPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(Pattern.compile(".*will_not_match.*"), HttpStatusClass.CLIENT_ERROR)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(Pattern.compile(".*will_not_match.*"), status)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusNotEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)

        def respBody = toStringAndClose(clientToProxy.execute(new HttpGet(url)).entity.content)
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody)

        def result = proxy.assertResponseStatusCode(Pattern.compile(".*will_not_match.*"), HttpStatus.SC_BAD_REQUEST)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    protected mockResponse(String path, Integer status) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${path}"),
                Times.once())
                .respond(response()
                .withStatusCode(status)
                .withBody(SUCCESSFUL_RESPONSE_BODY))
    }
}
