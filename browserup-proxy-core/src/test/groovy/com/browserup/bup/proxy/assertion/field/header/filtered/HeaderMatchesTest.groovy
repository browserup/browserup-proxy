package com.browserup.bup.proxy.assertion.field.header.filtered


import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.assertThat

// TODO
class HeaderMatchesTest extends FilteredHeaderBaseTest {

    @Test
    void filterMatchesBothRequestsAndFilteredHeadersContainValuePasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_BOTH, COMMON_HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void filterMatchesFirstRequestAndFilteredHeaderContainsValuePasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_FIRST, FIRST_HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void filterMatchesFirstRequestAndFilteredHeaderDoesNotContainValueFails() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_FIRST, SECOND_HEADER_VALUE)

        assertAssertionFailed(result)
    }

    @Test
    void filterMatchesNoRequestsPasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_NOTHING, SECOND_HEADER_VALUE)

        assertAssertionPassed(result)
        assertAssertionHasNoEntries(result)
    }

    @Test
    void filterMatchesBothRequestsAndFilteredHeadersDoNotContainValueFails() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_BOTH, ABSENT_HEADER_VALUE)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat('Expected to get both assertion entries', failedRequests, Matchers.hasSize(2))
    }

    @Test
    void filterMatchesBothRequestsAndSomeHeadersDoNotContainValueFails() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_BOTH, SECOND_HEADER_VALUE)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat('Expected to get both assertion entries',
                failedRequests, Matchers.hasSize(1))
        assertThat('Expected failed assertion entry to have proper url',
                failedRequests.get(0).url, Matchers.containsString(FIRST_URL_PATH))
    }

}
