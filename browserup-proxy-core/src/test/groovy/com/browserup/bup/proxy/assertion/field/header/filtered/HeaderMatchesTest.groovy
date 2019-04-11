package com.browserup.bup.proxy.assertion.field.header.filtered


import org.hamcrest.Matchers
import org.junit.Test

import java.util.regex.Pattern

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertThat

class HeaderMatchesTest extends FilteredHeaderBaseTest {

    @Test
    void urlFilterMatchesBothAndHeaderNameFilterMatchesBothAndHeaderValueFilterMatchesBothPasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderMatches(
                URL_PATTERN_TO_MATCH_BOTH,
                HEADER_NAME_PATTERN_TO_MATCH_BOTH,
                HEADER_VALUE_PATTERN_TO_MATCH_BOTH)

        assertAssertionPassed(result)
    }

    @Test
    void urlFilterMatchesBothAndAnyHeaderNameIsUsedAndHeaderValueFilterMatchesFirstFails() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderMatches(
                URL_PATTERN_TO_MATCH_BOTH,
                HEADER_VALUE_PATTERN_TO_MATCH_FIRST)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat('Expected to get one assertion entry', failedRequests, Matchers.hasSize(1))

        assertThat('Expected failed assertion entry to contain second url path',
                failedRequests.get(0).url, containsString(SECOND_URL_PATH))
    }

    @Test
    void urlFilterMatchesFirstAndAnyHeaderNameIsUsedAndHeaderValueFilterMatchesFirstPasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderMatches(
                URL_PATTERN_TO_MATCH_FIRST,
                HEADER_VALUE_PATTERN_TO_MATCH_FIRST)

        assertAssertionPassed(result)
    }

    @Test
    void urlFilterMatchesFirstAndAnyHeaderNameIsUsedAndHeaderValueFilterMatchesAllPasses() {
        def headerValuePattern = Pattern.compile(".*")

        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderMatches(
                URL_PATTERN_TO_MATCH_FIRST,
                headerValuePattern)

        assertAssertionPassed(result)
    }

    @Test
    void urlFilterMatchesNothingAndHeaderNameFilterMatchesFirstAndHeaderValueFilterMatchesFirstPasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderMatches(
                URL_PATTERN_TO_MATCH_NOTHING,
                HEADER_NAME_PATTERN_TO_MATCH_FIRST,
                HEADER_VALUE_PATTERN_TO_MATCH_FIRST)

        assertAssertionPassed(result)
    }

    @Test
    void urlFilterMatchesFirstAndHeaderNameFilterMatchesFirstAndHeaderValueFilterMatchesFirstPasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderMatches(
                URL_PATTERN_TO_MATCH_FIRST,
                HEADER_NAME_PATTERN_TO_MATCH_FIRST,
                HEADER_VALUE_PATTERN_TO_MATCH_FIRST)

        assertAssertionPassed(result)
    }

    @Test
    void urlFilterMatchesFirstAndHeaderNameFilterMatchesFirstAndHeaderValueFilterMatchesSecondFails() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderMatches(
                URL_PATTERN_TO_MATCH_FIRST,
                HEADER_NAME_PATTERN_TO_MATCH_FIRST,
                HEADER_VALUE_PATTERN_TO_MATCH_SECOND)

        assertAssertionFailed(result)

        def failedRequests = result.failedRequests

        assertThat('Expected one assertion entry', failedRequests, Matchers.hasSize(1))
        assertThat('Expected assertion entry to have proper url',
                failedRequests.get(0).url, containsString(FIRST_URL_PATH))
    }

    @Test
    void urlFilterMatchesFirstAndHeaderNameFilterMatchesNothingAndHeaderValueFilterMatchesSecondPasses() {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER)

        def result = proxy.assertAnyUrlResponseHeaderMatches(
                URL_PATTERN_TO_MATCH_FIRST,
                HEADER_NAME_PATTERN_TO_MATCH_NOTHING,
                HEADER_VALUE_PATTERN_TO_MATCH_SECOND)

        assertAssertionPassed(result)
    }
}
