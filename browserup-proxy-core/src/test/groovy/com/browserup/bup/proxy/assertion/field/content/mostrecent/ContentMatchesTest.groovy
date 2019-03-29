package com.browserup.bup.proxy.assertion.field.content.mostrecent

import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.*

class ContentMatchesTest extends MostRecentContentBaseTest {

    @Test
    void oldAndRecentMatchesAndRecentFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentMatches(
                RECENT_REQUEST_URL_PATH_PATTERN, BODY_PATTERN_TO_MATCH_BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void oldAndRecentMatchesAndOldFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentMatches(
                OLD_REQUEST_URL_PATH_PATTERN, BODY_PATTERN_TO_MATCH_BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void onlyOldMatchesAndOldFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentMatches(
                OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PATTERN_TO_MATCH_BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void onlyOldMatchesAndRecentFilterIsUsedFails() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentMatches(
                RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PATTERN_TO_MATCH_BODY_PART)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(RECENT_REQUEST_URL_PATH))
    }

    @Test
    void onlyRecentMatchesAndOldFilterIsUsedFails() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentMatches(
                OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PATTERN_TO_MATCH_BODY_PART)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(OLD_REQUEST_URL_PATH))
    }
}
