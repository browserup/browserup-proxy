/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.python

import com.browserup.bup.WithRunningProxyRestTest
import org.awaitility.Awaitility
import org.junit.After
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class PythonTestClient extends WithRunningProxyRestTest {
    private static final Logger LOG = LoggerFactory.getLogger(PythonTestClient)

    private GenericContainer container

    @Override
    String getUrlPath() {
        return 'har/entries'
    }

    @After
    void shutDown() {
        if (container != null) {
            container.stop()
        }
    }

    @Test
    void connectToProxySuccessfully() {
        def urlToCatch = 'test'
        def urlNotToCatch = 'missing'
        def responseBody = 'success'

        mockTargetServerResponse(urlToCatch, responseBody)
        mockTargetServerResponse(urlNotToCatch, responseBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlToCatch, responseBody)
        requestToTargetServer(urlNotToCatch, responseBody)

        Testcontainers.exposeHostPorts(restServer.connectors[0].localPort as Integer)
        Testcontainers.exposeHostPorts(proxy.port as Integer)

        def dockerfile = new File('./src/test/python/Dockerfile')
        container = new GenericContainer(
                new ImageFromDockerfile()
                        .withDockerfile(Paths.get(dockerfile.path)))
                        .withEnv('PROXY_REST_HOST', 'host.testcontainers.internal')
                        .withEnv('PROXY_REST_PORT', restServer.connectors[0].localPort as String)
                        .withEnv('PROXY_PORT', proxy.port as String)

        container.start()

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until({-> !container.isRunning()})

        LOG.info('Docker log: ' + container.getLogs())

        Assert.assertEquals("Expected python-client container exit code to be 0", 0, container.getCurrentContainerInfo().getState().getExitCode())
    }

    @Test
    void failsToConnectToProxy() {
        def urlToCatch = 'test'
        def urlNotToCatch = 'missing'
        def responseBody = 'success'
        def invalidProxyPort = 8

        mockTargetServerResponse(urlToCatch, responseBody)
        mockTargetServerResponse(urlNotToCatch, responseBody)

        proxyManager.get()[0].newHar()

        requestToTargetServer(urlToCatch, responseBody)
        requestToTargetServer(urlNotToCatch, responseBody)

        Testcontainers.exposeHostPorts(restServer.connectors[0].localPort as Integer)
        Testcontainers.exposeHostPorts(proxy.port as Integer)

        def dockerfile = new File('./src/test/python/Dockerfile')
        container = new GenericContainer(
                new ImageFromDockerfile()
                        .withDockerfile(Paths.get(dockerfile.path)))
                .withEnv('PROXY_REST_HOST', 'host.testcontainers.internal')
                .withEnv('PROXY_REST_PORT', restServer.connectors[0].localPort as String)
                .withEnv('PROXY_PORT', invalidProxyPort as String)

        container.start()

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until({-> !container.isRunning()})

        LOG.info('Docker log: ' + container.getLogs())

        Assert.assertEquals("Expected python-client container exit code to be 1", 1, container.getCurrentContainerInfo().getState().getExitCode())
    }
}

