package com.browserup.bup.proxy.assertion.field.content.filtered


import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.*

class ContentDoesNotContainTest extends FilteredContentBaseTest {

    @Test
    void filterMatchesBothRequestsAndBothContentDoNotContainTextPasses() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertAnyUrlContentDoesNotContain(URL_PATTERN_TO_MATCH_BOTH, BODY_PART)

        assertTrue("Expected assertion to pass", result.passed)
        assertFalse("Expected assertion to pass", result.failed)
    }

    @Test
    void filterMatchesFirstRequestAndOnlySecondContentDoesNotContainTextFails() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertAnyUrlContentDoesNotContain(URL_PATTERN_TO_MATCH_FIRST, BODY_PART)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(FIRST_URL_PATH))
    }

    @Test
    void filterMatchesBothRequestsAndSomeContentContainTextFails_1() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertAnyUrlContentDoesNotContain(URL_PATTERN_TO_MATCH_BOTH, BODY_PART)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(SECOND_URL_PATH))
    }

    @Test
    void filterMatchesBothRequestsAndSomeContentContainTextFails_2() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertAnyUrlContentDoesNotContain(URL_PATTERN_TO_MATCH_BOTH, BODY_PART)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(FIRST_URL_PATH))
    }
}
