/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup

import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.ProxyManager
import com.browserup.bup.proxy.bricks.ProxyResource
import com.browserup.bup.proxy.guice.ConfigModule
import com.browserup.bup.proxy.guice.JettyModule
import com.browserup.bup.util.BrowserUpProxyUtil
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.servlet.GuiceServletContextListener
import com.google.sitebricks.SitebricksModule
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.entity.ContentType
import org.awaitility.Awaitility
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.ServletContextEvent
import java.util.concurrent.TimeUnit

abstract class WithRunningProxyRestTest {
    private static final Logger LOG = LoggerFactory.getLogger(WithRunningProxyRestTest)

    protected ProxyManager proxyManager
    protected BrowserUpProxyServer proxy
    protected Server restServer

    protected String[] getArgs() {
        ['--port', '0'] as String[]
    }

    abstract String getUrlPath();

    String getFullUrlPath() {
        return "/proxy/${proxy.port}/${urlPath}"
    }

    protected int mockServerPort
    protected int mockServerHttpsPort

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().port(0).httpsPort(0))

    @Before
    void setUp() throws Exception {
        Injector injector = Guice.createInjector(new ConfigModule(args), new JettyModule(), new SitebricksModule() {
            @Override
            protected void configureSitebricks() {
                scan(ProxyResource.class.getPackage())
            }
        })

        proxyManager = injector.getInstance(ProxyManager)

        LOG.debug("Starting BrowserUp Proxy version " + BrowserUpProxyUtil.versionString)

        new Thread(new Runnable() {
            @Override
            void run() {
                startRestServer(injector)
            }
        }).start()

        LOG.debug("Waiting till BrowserUp Rest server is started")

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until({ -> restServer != null && restServer.isStarted() })

        LOG.debug("BrowserUp Rest server is started successfully")

        LOG.debug("Waiting till BrowserUp Proxy server is started")

        proxy = proxyManager.create(0)

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> proxyManager.get().size() > 0 })

        LOG.debug("BrowserUp Proxy server is started successfully")

        mockServerPort = wireMockRule.port();
        mockServerHttpsPort = wireMockRule.httpsPort();

        waitForProxyServer()
    }

    def waitForProxyServer() {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ ->
            def successful = false
            proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
                uri.path = "/proxy"
                response.success = { _, reader ->
                    successful = true
                }
                response.failure = { _, reader ->
                    successful = false
                }
            }
            return successful
        })
    }

    HTTPBuilder getTargetServerClient() {
        def http = new HTTPBuilder("http://localhost:${mockServerPort}")
        http.setProxy('localhost', proxy.port, 'http')
        http
    }

    HTTPBuilder getProxyRestServerClient() {
        new HTTPBuilder("http://localhost:${restServer.connectors[0].localPort}")
    }

    def sendGetToProxyServer(Closure configClosure) {
        proxyRestServerClient.request(Method.GET, ContentType.WILDCARD, configClosure)
    }

    void requestToTargetServer(url, expectedResponse) {
        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${url}"
            response.success = { _, reader ->
                Assert.assertEquals(expectedResponse, reader.text)
            }
            response.failure = { _, reader ->
                Assert.assertEquals(expectedResponse, reader.text)
            }
        }
    }

    @After
    void tearDown() throws Exception {
        LOG.debug('Stopping proxy servers')
        for (def proxyServer : proxyManager.get()) {
            try {
                proxyManager.delete(proxyServer.port)
            } catch (Exception ex) {
                LOG.error('Error while stopping proxy servers', ex)
            }
        }

        if (restServer != null) {
            LOG.debug('Stopping rest proxy server')
            try {
                restServer.stop()
            } catch (Exception ex) {
                LOG.error('Error while stopping rest proxy server', ex)
            }
        }
    }

    private void startRestServer(Injector injector) {
        restServer = injector.getInstance(Server.class)
        def contextListener = new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return injector
            }
        }
        restServer.start()
        contextListener.contextInitialized(
                new ServletContextEvent((restServer.handler as ServletContextHandler).servletContext))
        try {
            restServer.join()
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt()
        }
    }

    protected void mockTargetServerResponse(String url, String responseBody, int delayMilliseconds=0) {
        def response = WireMock.aResponse().withStatus(200)
                .withBody(responseBody)
                .withHeader('Content-Type', 'text/plain')
                .withFixedDelay(delayMilliseconds)
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/${url}")).willReturn(response))
    }
}
