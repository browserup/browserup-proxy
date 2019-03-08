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
import org.awaitility.Awaitility
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.junit.After
import org.junit.Before
import org.mockserver.integration.ClientAndServer

import javax.servlet.ServletContextEvent
import java.util.concurrent.TimeUnit

class WithRunningProxyRestTest {
    protected ProxyManager proxyManager
    protected BrowserUpProxyServer proxy
    protected ClientAndServer targetMockedServer
    protected Server restServer

    protected String[] getArgs() {
        ['--use-littleproxy', 'true', '--port', '0'] as String[]
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

        println("Starting BrowserUp Proxy version " + BrowserUpProxyUtil.versionString)

        new Thread(new Runnable() {
            @Override
            void run() {
                startRestServer(injector)
            }
        }).start()

        println("Waiting till BrowserUp Rest server is started")

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until({ -> restServer != null && restServer.isStarted() })

        println("BrowserUp Rest server is started successfully")

        println("Waiting till BrowserUp Proxy server is started")

        proxy = proxyManager.create(0)

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until({ -> proxyManager.get().size() > 0 })

        println("BrowserUp Proxy server is started successfully")

        targetMockedServer = new ClientAndServer(0)

    }

    HTTPBuilder getTargetServerHttpBuilder() {
        def http = new HTTPBuilder("http://localhost:${targetMockedServer.port}")
        http.setProxy('localhost', proxy.port, 'http')
        http
    }

    HTTPBuilder getProxyRestServerHttpBuilder() {
        def http = new HTTPBuilder("http://localhost:${restServer.connectors[0].localPort}")
        http
    }

    @After
    void tearDown() throws Exception {
        for (def proxyServer : proxyManager.get()) {
            try {
                proxyManager.delete(proxyServer.port)
            } catch (Exception ignored) {
            }
        }
        if (!proxy.isStopped()) {
            proxy.stop()
        }
        if (targetMockedServer != null) {
            targetMockedServer.stop()
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
}