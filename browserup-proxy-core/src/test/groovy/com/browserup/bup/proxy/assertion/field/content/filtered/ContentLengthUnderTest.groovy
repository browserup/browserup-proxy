package com.browserup.bup.proxy.assertion.field.content.filtered


import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.assertThat

class ContentLengthUnderTest extends FilteredContentBaseTest {
    private static final String BIG_BODY = (1..10).collect { 'big body' }.join(' ')
    private static final String SMALL_BODY = 'small body'
    private static final Long BIG_BODY_SIZE = BIG_BODY.bytes.length
    private static final Long SMALL_BODY_SIZE = SMALL_BODY.bytes.length

    @Test
    void filterMatchesBothRequestsAndBothContentLengthAreUnderLimitPasses() {
        mockAndSendRequestsToMockedServer(BIG_BODY, SMALL_BODY)

        def result = proxy.assertAnyUrlContentLengthUnder(URL_PATTERN_TO_MATCH_BOTH, BIG_BODY_SIZE)

        assertAssertionPassed(result)
    }

    @Test
    void filterMatchesFirstRequestAndOnlySecondContentLengthIsUnderLimitFails() {
        mockAndSendRequestsToMockedServer(BIG_BODY, SMALL_BODY)

        def result = proxy.assertAnyUrlContentLengthUnder(URL_PATTERN_TO_MATCH_FIRST, SMALL_BODY_SIZE)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(FIRST_URL_PATH))
    }

    @Test
    void filterMatchesBothRequestsAndSomeContentIsNotUnderLimitFails() {
        mockAndSendRequestsToMockedServer(BIG_BODY, SMALL_BODY)

        def result = proxy.assertAnyUrlContentLengthUnder(URL_PATTERN_TO_MATCH_BOTH, SMALL_BODY_SIZE)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected there's one failed assertion entry", failedRequests, Matchers.hasSize(1))
        assertThat("Expected failed entry has proper url",
                failedRequests.get(0).url,
                Matchers.containsString(FIRST_URL_PATH))
    }

    @Test
    void filterMatchesBothRequestsAndAllContentIsNotUnderLimitFails() {
        mockAndSendRequestsToMockedServer(BIG_BODY, SMALL_BODY)

        def result = proxy.assertAnyUrlContentLengthUnder(URL_PATTERN_TO_MATCH_BOTH, SMALL_BODY_SIZE - 1)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat("Expected to find both assertion entries", failedRequests, Matchers.hasSize(2))
    }
}
