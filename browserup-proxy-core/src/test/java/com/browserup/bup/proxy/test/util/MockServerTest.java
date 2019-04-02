/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy.test.util;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Tests can subclass this to get access to a ClientAndServer instance for creating mock responses.
 */
public class MockServerTest {
    protected int mockServerPort;
    protected int mockServerHttpsPort;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(0).httpsPort(0));

    @Before
    public void setUpMockServer() {
        mockServerPort = wireMockRule.port();
        mockServerHttpsPort = wireMockRule.httpsPort();
    }
}
