package com.browserup.bup.proxy.assertion.field.header.filtered


import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.assertThat

class HeaderDoesNotContainTest extends FilteredHeaderBaseTest {

    @Test
    void filterMatchesBothRequestsAndFilteredHeadersDoNotContainValuePasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderDoesNotContain(URL_PATTERN_TO_MATCH_BOTH, ABSENT_HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void filterMatchesFirstRequestAndFilteredHeaderDoesNotContainValuePasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderDoesNotContain(URL_PATTERN_TO_MATCH_FIRST, SECOND_HEADER_VALUE)

        assertAssertionPassed(result)
    }

    @Test
    void filterMatchesFirstRequestAndFilteredHeaderContainValueFails() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderDoesNotContain(URL_PATTERN_TO_MATCH_FIRST, FIRST_HEADER_VALUE)

        assertAssertionFailed(result)
    }

    @Test
    void filterMatchesNoRequestsPasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderDoesNotContain(URL_PATTERN_TO_MATCH_NOTHING, SECOND_HEADER_VALUE)

        assertAssertionPassed(result)
        assertAssertionHasNoEntries(result)
    }

    @Test
    void filterMatchesBothRequestsAndFilteredHeadersContainValueFails() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderDoesNotContain(URL_PATTERN_TO_MATCH_BOTH, COMMON_HEADER_VALUE)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat('Expected to get both assertion entries', failedRequests, Matchers.hasSize(2))
    }

    @Test
    void filterMatchesBothRequestsAndSomeHeadersContainValueFails() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderDoesNotContain(URL_PATTERN_TO_MATCH_BOTH, SECOND_HEADER_VALUE)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat('Expected to get both assertion entries',
                failedRequests, Matchers.hasSize(1))
        assertThat('Expected failed assertion entry to have proper url',
                failedRequests.get(0).url, Matchers.containsString(SECOND_URL_PATH))
    }

}
