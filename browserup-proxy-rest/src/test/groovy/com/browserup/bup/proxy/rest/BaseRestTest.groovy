package com.browserup.bup.proxy.rest

import com.browserup.bup.assertion.model.AssertionResult
import org.mockserver.model.Delay

import static org.junit.Assert.*

abstract class BaseRestTest extends WithRunningProxyRestTest {
    protected static final int TARGET_SERVER_RESPONSE_DELAY = 500
    protected static final int TARGET_SERVER_SLOW_RESPONSE_DELAY = 1000
    protected static final int SUCCESSFUL_ASSERTION_TIME_WITHIN = TARGET_SERVER_RESPONSE_DELAY + 100
    protected static final int FAILED_ASSERTION_TIME_WITHIN = TARGET_SERVER_RESPONSE_DELAY - 100
    protected static final int MILLISECONDS_BETWEEN_REQUESTS = 50

    abstract String getUrlPath();

    String getFullUrlPath() {
        return "/proxy/${proxy.port}/${urlPath}"
    }

    static def assertAssertionNotNull(AssertionResult assertion) {
        assertNotNull('Expected to get non null assertion result', assertion)
    }

    static def assertAssertionPassed(AssertionResult assertion) {
        assertTrue("Expected assertion to pass", assertion.passed)
        assertFalse("Expected assertion to pass", assertion.failed)
    }

    static def assertAssertionFailed(AssertionResult assertion) {
        assertFalse("Expected assertion to fail", assertion.passed)
        assertTrue("Expected assertion to fail", assertion.failed)
    }
}
