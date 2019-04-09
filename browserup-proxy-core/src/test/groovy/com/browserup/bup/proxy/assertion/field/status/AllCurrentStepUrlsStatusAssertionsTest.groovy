package com.browserup.bup.proxy.assertion.field.status

import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import com.browserup.bup.util.HttpStatusClass
import org.apache.http.HttpStatus
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.*

class AllCurrentStepUrlsStatusAssertionsTest extends BaseAssertionsTest {

    @Test
    void currentUrlsStatusCodesBelongToClassPass() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)
        mockResponse(URL_PATH, status)

        requestToMockedServer(URL_PATH)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertResponseStatusCode(HttpStatusClass.SUCCESS)

        assertTrue("Expected statuses of all responses to have the same class", result.passed)
        assertFalse("Expected statuses of all responses to have the same class", result.failed)
    }

    @Test
    void currentUrlsStatusCodesBelongToClassFail() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)
        mockResponse(URL_PATH, HttpStatus.SC_NOT_FOUND)

        requestToMockedServer(URL_PATH)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertResponseStatusCode(HttpStatusClass.SUCCESS)

        assertFalse("Expected some response statuses to belong to other classes", result.passed)
        assertTrue("Expected some response statuses to belong to other classes" , result.failed)
    }

    @Test
    void currentUrlsStatusCodesEqualPass() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)
        mockResponse(URL_PATH, status)

        requestToMockedServer(URL_PATH)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertResponseStatusCode(status)

        assertTrue("Expected statuses of all responses to match", result.passed)
        assertFalse("Expected statuses of all responses to match", result.failed)
    }

    @Test
    void currentUrlsStatusCodesEqualFail() {
        def status = HttpStatus.SC_OK

        mockResponse(URL_PATH, status)
        mockResponse(URL_PATH, HttpStatus.SC_NOT_FOUND)

        requestToMockedServer(URL_PATH)
        requestToMockedServer(URL_PATH)

        def result = proxy.assertResponseStatusCode(status)

        assertFalse("Expected some responses statuses not to match", result.passed)
        assertTrue("Expected some responses statuses not to match", result.failed)
    }

    protected mockResponse(String path, Integer status) {
        stubFor(get(urlEqualTo("/${path}")).
                willReturn(
                        aResponse().withStatus(status).withBody(SUCCESSFUL_RESPONSE_BODY)
                )
        )
    }
}
