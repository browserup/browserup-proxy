package com.browserup.bup.proxy.rest

import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.ProxyManager
import com.browserup.bup.proxy.bricks.ProxyResource
import com.browserup.bup.proxy.guice.ConfigModule
import com.browserup.bup.proxy.guice.JettyModule
import com.browserup.bup.util.BrowserUpProxyUtil
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.servlet.GuiceServletContextListener
import com.google.sitebricks.SitebricksModule
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.eclipse.jetty.http.HttpMethods
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.junit.After
import org.junit.Before
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Delay
import org.mockserver.model.Header
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.ServletContextEvent
import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertEquals
import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class WithRunningProxyRestTest {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyManager)

    protected ProxyManager proxyManager
    protected BrowserUpProxyServer proxy
    protected ClientAndServer targetMockedServer
    protected Server restServer

    protected String[] getArgs() {
        ['--port', '0'] as String[]
    }

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

        targetMockedServer = new ClientAndServer(0)

    }

    HTTPBuilder getTargetServerClient() {
        def http = new HTTPBuilder("http://localhost:${targetMockedServer.port}")
        http.setProxy('localhost', proxy.port, 'http')
        http
    }

    HTTPBuilder getProxyRestServerClient() {
        new HTTPBuilder("http://localhost:${restServer.connectors[0].localPort}")
    }

    void requestToTargetServer(url, expectedResponse) {
        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.path = "/${url}"
            response.success = { _, reader ->
                assertEquals(expectedResponse, reader.text)
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
        if (targetMockedServer != null) {
            LOG.debug('Stopping target mocked server')
            targetMockedServer.stop()
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

    protected void mockTargetServerResponse(String url, String responseBody, Delay delay=Delay.milliseconds(0)) {
        targetMockedServer.when(request()
                .withMethod(HttpMethods.GET)
                .withPath("/${url}"),
                Times.exactly(1))
                .respond(response()
                .withStatusCode(HttpStatus.SC_OK)
                .withDelay(delay)
                .withHeader(new Header(HttpHeaders.CONTENT_TYPE, 'text/plain'))
                .withBody(responseBody))
    }
}