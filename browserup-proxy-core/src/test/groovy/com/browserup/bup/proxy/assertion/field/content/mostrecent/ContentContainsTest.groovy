package com.browserup.bup.proxy.assertion.field.content.mostrecent

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ContentContainsTest extends MostRecentContentBaseTest {

    @Test
    void oldAndRecentContainsTextAndRecentFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentContains(RECENT_REQUEST_URL_PATH_PATTERN, BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void oldAndRecentContainsTextAndOldFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentContains(OLD_REQUEST_URL_PATH_PATTERN, BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void onlyOldContainsTextAndOldFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentContains(OLD_REQUEST_URL_PATH_PATTERN, BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void onlyOldContainsTextAndRecentFilterIsUsedFails() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentContains(RECENT_REQUEST_URL_PATH_PATTERN, BODY_PART)

        assertAssertionFailed(result)
    }
}
