package com.browserup.bup.proxy.assertion.field.content

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import org.apache.http.HttpStatus
import org.junit.Before
import org.mockserver.matchers.Times

import java.util.regex.Pattern

import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class ContentBaseTest extends BaseAssertionsTest {
    protected static final String BODY_PART = 'body part'
    protected static final String BODY_CONTAINING_BODY_PART = "body example with ${BODY_PART} in the middle".toString()
    protected static final String BODY_NOT_CONTAINING_BODY_PART = 'body example'
    protected static final Pattern BODY_PATTERN_TO_MATCH_BODY_PART = Pattern.compile(".*${BODY_PART}.*")
    protected static final String BODY_PATTERN_NOT_TO_MATCH_BODY_PART = Pattern.compile(".*NOT-TO-MATCH.*")

    @Before
    void setUp() {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_CONTENT)
    }

    protected mockResponse(String path, String body) {
        mockServer.when(request()
                .withMethod("GET")
                .withPath("/${path}"),
                Times.once())
                .respond(response()
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(body))
    }
}
