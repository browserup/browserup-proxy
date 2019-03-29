package com.browserup.bup.proxy.assertion.field.content.mostrecent

import com.browserup.bup.proxy.assertion.field.content.ContentBaseTest

import java.util.regex.Pattern

class MostRecentContentBaseTest extends ContentBaseTest {
    protected static final String RECENT_REQUEST_URL_PATH = "recent-${URL_PATH}"
    protected static final Pattern RECENT_REQUEST_URL_PATH_PATTERN = Pattern.compile(".*${RECENT_REQUEST_URL_PATH}.*")
    protected static final String OLD_REQUEST_URL_PATH = "old-${URL_PATH}"
    protected static final Pattern OLD_REQUEST_URL_PATH_PATTERN = Pattern.compile(".*${OLD_REQUEST_URL_PATH}.*")
    protected static final Integer DELAY_BETWEEN_REQUESTS = 50
    protected static final String RECENT_BODY = "recent body"
    protected static final String OLD_BODY = 'old body'
    protected static final Pattern BODY_PATTERN_TO_MATCH_RECENT = Pattern.compile(".*${RECENT_BODY}.*")
    protected static final Pattern BODY_PATTERN_TO_MATCH_OLD = Pattern.compile(".*${OLD_BODY}.*")

    protected mockAndSendRequestsToMockedServer(String recentBody, String oldBody) {
        mockResponse(RECENT_REQUEST_URL_PATH, recentBody)
        mockResponse(OLD_REQUEST_URL_PATH, oldBody)

        requestToMockedServer(OLD_REQUEST_URL_PATH, oldBody)
        sleep DELAY_BETWEEN_REQUESTS
        requestToMockedServer(RECENT_REQUEST_URL_PATH, recentBody)
    }
}
