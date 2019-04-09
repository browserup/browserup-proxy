package com.browserup.bup.proxy.assertion.field.content

import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import org.apache.http.HttpHeaders
import org.eclipse.jetty.http.MimeTypes
import org.junit.Before

import java.util.regex.Pattern

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

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
        stubFor(get(urlEqualTo('/' + path)).
                willReturn(
                        ok().
                                withHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString()).
                                withBody(body))
        )
    }
}
