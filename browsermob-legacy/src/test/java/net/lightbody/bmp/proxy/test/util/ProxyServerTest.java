package net.lightbody.bmp.proxy.test.util;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.proxy.LegacyProxyServer;
import net.lightbody.bmp.proxy.ProxyServer;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * Extend this class to gain access to a local proxy server. If you need both a local proxy server and a local Jetty server, extend
 * {@link net.lightbody.bmp.proxy.test.util.LocalServerTest} instead.
 * <p/>
 * Call getNewHttpClient() to get an HttpClient that can be used to make requests via the local proxy.
 */
public abstract class ProxyServerTest {
    private static final Logger log = LoggerFactory.getLogger(ProxyServerTest.class);

    /**
     * The port the local proxy server is currently running on.
     */
    protected int proxyServerPort;

    /**
     * This test's proxy server, running on 127.0.0.1.
     */
    protected LegacyProxyServer proxy;

    /**
     * This test's proxy server, running on 127.0.0.1. This is the same instance as the proxy.
     */
    protected BrowserMobProxy browserMobProxy;

    /**
     * CloseableHttpClient that will connect through the local proxy running on 127.0.0.1.
     */
    protected CloseableHttpClient client;

    /**
     * CookieStore managing this instance's HttpClient's cookies.
     */
    protected CookieStore cookieStore;

    @Before
    public void startProxyServer() throws Exception {
        this.proxy = createProxyServer();
        this.browserMobProxy = (BrowserMobProxy) this.proxy;
        proxy.start();
        proxyServerPort = proxy.getPort();

        cookieStore = new BasicCookieStore();
        client = ProxyServerTestUtil.getNewHttpClient(proxyServerPort, cookieStore);
    }

    /**
     * Hook to allow tests to initialize the proxy server with a custom configuration, but still leverage the rest of the
     * functionality in ProxyServerTest. The default implementation creates a new proxy server on port 0 (JVM-assigned port).
     */
    protected LegacyProxyServer createProxyServer() {
        if (Boolean.getBoolean("bmp.use.littleproxy")) {
            // HACK! since browsermob-core has no knowledge of littleproxy, we have to use reflection to grab the LP implementation
            try {
                Class<LegacyProxyServer> littleProxyImplClass = (Class<LegacyProxyServer>) Class.forName("net.lightbody.bmp.BrowserMobProxyServerLegacyAdapter");
                LegacyProxyServer littleProxyImpl = littleProxyImplClass.newInstance();

                // set the trustAllServers option on the LP implementation to "true", to avoid certificate verification issues with MITM
                Method setTrustAllServersMethod = littleProxyImplClass.getMethod("setTrustAllServers", Boolean.TYPE);
                setTrustAllServersMethod.invoke(littleProxyImpl, true);

                log.info("Using LittleProxy implementation to execute test for class: " + getClass().getSimpleName());
                return littleProxyImpl;
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("The System property bmp.use.littleproxy was true, but the LittleProxy implementation could not be loaded.", e);
            }
        } else {
            return new ProxyServer(0);
        }
    }

    @After
    public void stopProxyServer() throws Exception {
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            if (proxy != null) {
                proxy.abort();
            }
        }
    }

    /**
     * Convenience method to perform an HTTP GET to the specified URL and return the response body. Closes the response before returning
     * the body.
     *
     * @param url URL to HTTP GET
     * @return response body from the server
     */
    public String getResponseBodyFromHost(String url) {
        try {
            CloseableHttpResponse response = getResponseFromHost(url);

            String body = IOUtils.toStringAndClose(response.getEntity().getContent());

            response.close();

            return body;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method to perform an HTTP GET to the specified URL and return the response object. The response is not closed, and so
     * MUST be closed by the calling code.
     *
     * @param url URL to HTTP GET
     * @return CloseableHttpResponse from the server
     */
    public CloseableHttpResponse getResponseFromHost(String url) {
        HttpGet httpGet = new HttpGet(url);
        try {
            return client.execute(httpGet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
