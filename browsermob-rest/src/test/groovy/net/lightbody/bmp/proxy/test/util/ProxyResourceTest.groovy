package net.lightbody.bmp.proxy.test.util

import com.google.sitebricks.headless.Request
import groovyx.net.http.HTTPBuilder
import net.lightbody.bmp.proxy.LegacyProxyServer
import net.lightbody.bmp.proxy.bricks.ProxyResource
import org.junit.After
import org.junit.Before
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockserver.integration.ClientAndServer

import java.nio.charset.StandardCharsets

import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

abstract class ProxyResourceTest extends ProxyManagerTest {
    ProxyResource proxyResource
    int proxyPort
    protected ClientAndServer mockServer;
    protected int mockServerPort;

    @Before
    public void setUpMockServer() {
        mockServer = new ClientAndServer(0);
        mockServerPort = mockServer.getPort();
    }

    @After
    public void tearDownMockServer() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Before
    void setUpProxyResource() {
        LegacyProxyServer proxy = proxyManager.create(0)
        proxyPort = proxy.port

        proxyResource = new ProxyResource(proxyManager)
    }

    public HTTPBuilder getHttpBuilder() {
        def http = new HTTPBuilder("http://localhost:${mockServerPort}")
        http.setProxy("localhost", proxyPort, "http")

        return http
    }

    /**
     * Creates a mock sitebricks REST request with the specified entity body.
     */
    public static Request<String> createMockRestRequestWithEntity(String entityBody) {
        Request<String> mockRestRequest = mock(Request)
        when(mockRestRequest.header("Content-Type")).thenReturn("text/plain; charset=utf-8")
        when(mockRestRequest.readTo(any(OutputStream))).then(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                OutputStream os = invocationOnMock.getArguments()[0]
                os.write(entityBody.getBytes(StandardCharsets.UTF_8))

                return null
            }
        })
        mockRestRequest
    }
}
