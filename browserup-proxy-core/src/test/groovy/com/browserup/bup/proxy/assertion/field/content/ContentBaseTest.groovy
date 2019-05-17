package com.browserup.bup.proxy.assertion.field.content

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import org.apache.http.HttpStatus
import org.junit.Before
import org.mockserver.matchers.Times

import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class ContentBaseTest extends BaseAssertionsTest {
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
                        .withHeader("Content-Type", "application/json")
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody(body)
                )
    }
}
