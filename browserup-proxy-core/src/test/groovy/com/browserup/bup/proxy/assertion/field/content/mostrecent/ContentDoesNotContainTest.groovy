package com.browserup.bup.proxy.assertion.field.content.mostrecent

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ContentDoesNotContainTest extends MostRecentContentBaseTest {

    @Test
    void oldAndRecentDoNotContainTextAndRecentFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentDoesNotContain(RECENT_REQUEST_URL_PATH_PATTERN, BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void oldAndRecentDoNotContainTextAndOldFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentDoesNotContain(OLD_REQUEST_URL_PATH_PATTERN, BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void onlyOldDoesNotContainTextAndOldFilterIsUsedPasses() {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentDoesNotContain(OLD_REQUEST_URL_PATH_PATTERN, BODY_PART)

        assertAssertionPassed(result)
    }

    @Test
    void onlyOldDoesNotContainTextAndRecentFilterIsUsedFails() {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART)

        def result = proxy.assertMostRecentResponseContentDoesNotContain(RECENT_REQUEST_URL_PATH_PATTERN, BODY_PART)

        assertAssertionFailed(result)
    }
}
