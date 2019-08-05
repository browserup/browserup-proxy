/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.ruby

import com.browserup.bup.WithRunningProxyRestTest
import org.awaitility.Awaitility
import org.junit.Assert
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

import java.nio.file.Path
import java.util.concurrent.TimeUnit

class RubyClientTest extends WithRunningProxyRestTest {
    private static final Logger LOG = LoggerFactory.getLogger(RubyClientTest)

    @Override
    String getUrlPath() {
        return 'har/entries'
    }

    @Test
    void connectToProxy() {
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

        def dockerfile = new File('./src/test/ruby/Dockerfile')
        def container = new GenericContainer(
                new ImageFromDockerfile()
                        .withDockerfile(Path.of(dockerfile.path)))
                        .withEnv('PROXY_REST_HOST', 'host.testcontainers.internal')
                        .withEnv('PROXY_REST_PORT', restServer.connectors[0].localPort as String)
                        .withEnv('PROXY_PORT', proxy.port as String)

        container.start()

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until({-> !container.isRunning()})

        LOG.info('Docker log: ' + container.getLogs())

        Assert.assertEquals("Expected ruby-client container exit code to be 0", 0, container.getCurrentContainerInfo().getState().getExitCode())
    }
}
