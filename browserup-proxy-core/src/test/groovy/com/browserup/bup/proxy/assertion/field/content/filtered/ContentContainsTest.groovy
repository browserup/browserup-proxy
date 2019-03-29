package com.browserup.bup.proxy.assertion.field.content.filtered

import com.browserup.bup.proxy.assertion.field.content.ContentBaseTest
import org.hamcrest.Matchers
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class ContentContainsTest extends FilteredContentBaseTest {

    @Test
    void filterMatchesBothRequestsAndBothContentContainTextPasses() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertAnyUrlContentContains(URL_PATTERN_TO_MATCH_BOTH, BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void filterMatchesFirstRequestAndOnlySecondContentContainTextFails() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertAnyUrlContentContains(URL_PATTERN_TO_MATCH_FIRST, BODY_PART)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(FIRST_URL_PATH))
    }

    @Test
    void filterMatchesBothRequestsAndSomeContentDoesNotContainTextFails_1() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertAnyUrlContentContains(URL_PATTERN_TO_MATCH_BOTH, BODY_PART)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(SECOND_URL_PATH))
    }

    @Test
    void filterMatchesBothRequestsAndSomeContentDoesNotContainTextFails_2() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertAnyUrlContentContains(URL_PATTERN_TO_MATCH_BOTH, BODY_PART)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(FIRST_URL_PATH))
    }
}
