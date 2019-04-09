package com.browserup.bup.proxy.assertion.field.status

import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import com.browserup.bup.util.HttpStatusClass
import org.apache.http.HttpStatus
import org.hamcrest.Matchers
import org.junit.Test

import java.util.regex.Pattern

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.junit.Assert.*

class FilteredUrlsStatusAssertionsTest extends BaseAssertionsTest {
    protected static final Pattern COMMON_URL_PATTERN = Pattern.compile(".*some-url.*")
    protected static final Pattern NOT_TO_MATCH_PATTERN = Pattern.compile(".*will_not_match.*")
    protected static final String RECENT_PATH = "recent-some-url"
    protected static final String OLD_PATH = "old-some-url"

    @Test
    void filteredUrlsStatusesCodeBelongToClassPasses() {
        mockResponse(OLD_PATH, HttpStatus.SC_OK)
        mockResponse(RECENT_PATH, HttpStatus.SC_CREATED)

        requestToMockedServer(OLD_PATH)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertResponseStatusCode(COMMON_URL_PATTERN, HttpStatusClass.SUCCESS)

        assertThat("Expected to get proper assertion entries number", result.requests, Matchers.hasSize(2))
        assertTrue("Expected filtered urls statuses to belong to specified class", result.passed)
        assertFalse("Expected filtered urls statuses to belong to specified class", result.failed)
    }

    @Test
    void filteredUrlsStatusesCodeBelongToClassFail() {
        mockResponse(OLD_PATH, HttpStatus.SC_OK)
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST)

        requestToMockedServer(OLD_PATH)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertResponseStatusCode(COMMON_URL_PATTERN, HttpStatusClass.SUCCESS)

        assertThat("Expected to get proper assertion entries number", result.requests, Matchers.hasSize(2))

        def failedRequest = result.requests.findAll { it.failed }

        assertThat("Expected to get one failed assertion entry", failedRequest, Matchers.hasSize(1))
        assertThat("Expected failed assertion entry to have proper url", failedRequest.get(0).url, Matchers.containsString(RECENT_PATH))
        assertFalse("Expected some of filtered urls statuses not to belong to specified class", result.passed)
        assertTrue("Expected some of filtered urls statuses not to belong to specified class", result.failed)
    }


    @Test
    void filteredUrlStatusCodeEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, status)

        requestToMockedServer(OLD_PATH)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertResponseStatusCode(COMMON_URL_PATTERN, status)

        assertThat("Expected to get proper assertion entries number", result.requests, Matchers.hasSize(2))
        assertTrue("Expected filtered urls statuses to have specified status", result.passed)
        assertFalse("Expected filtered urls statuses to have specified status", result.failed)
    }

    @Test
    void filteredUrlStatusCodeEqualsFails() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, HttpStatus.SC_CREATED)

        requestToMockedServer(OLD_PATH)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertResponseStatusCode(COMMON_URL_PATTERN, status)

        assertThat("Expected to get proper assertion entries number", result.requests, Matchers.hasSize(2))

        def failedRequest = result.requests.findAll { it.failed }

        assertThat("Expected to get one failed assertion entry", failedRequest, Matchers.hasSize(1))
        assertThat("Expected failed assertion entry to have proper url", failedRequest.get(0).url, Matchers.containsString(RECENT_PATH))
        assertFalse("Expected some of filtered urls statuses not be equal to specified class", result.passed)
        assertTrue("Expected some of filtered urls statuses not be equal to specified class", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusBelongsToClassPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, status)

        requestToMockedServer(OLD_PATH)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertResponseStatusCode(NOT_TO_MATCH_PATTERN, HttpStatusClass.SUCCESS)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusDoesNotBelongToClassPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST)

        requestToMockedServer(OLD_PATH)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertResponseStatusCode(NOT_TO_MATCH_PATTERN, HttpStatusClass.SUCCESS)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, status)

        requestToMockedServer(OLD_PATH)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertResponseStatusCode(NOT_TO_MATCH_PATTERN, status)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusNotEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST)

        requestToMockedServer(OLD_PATH)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertResponseStatusCode(NOT_TO_MATCH_PATTERN, status)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    protected mockResponse(String path, Integer status) {
        stubFor(get(urlEqualTo("/${path}")).
                willReturn(
                        aResponse().
                                withStatus(status).
                                withBody(SUCCESSFUL_RESPONSE_BODY).
                                withHeader('Content-Type', 'text/plain; charset=UTF-8')
                )
        )
    }
}
