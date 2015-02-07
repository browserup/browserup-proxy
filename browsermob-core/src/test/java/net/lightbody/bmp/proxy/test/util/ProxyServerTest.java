package net.lightbody.bmp.proxy.test.util;

import net.lightbody.bmp.proxy.ProxyServer;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Extend this class to gain access to a local proxy server. If you need both a local proxy server and a local Jetty server, extend
 * {@link net.lightbody.bmp.proxy.test.util.LocalServerTest} instead.
 * <p/>
 * Call getNewHttpClient() to get an HttpClient that can be used to make requests via the local proxy.
 */
public abstract class ProxyServerTest {
    /**
     * The port the local proxy server is currently running on.
     */
    protected int proxyServerPort;

    /**
     * This test's proxy server, running on 127.0.0.1.
     */
    protected ProxyServer proxy;

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
        proxy.start();
        proxyServerPort = proxy.getPort();

        cookieStore = new BasicCookieStore();
        client = getNewHttpClient(proxyServerPort, cookieStore);
    }

    /**
     * Hook to allow tests to initialize the proxy server with a custom configuration, but still leverage the rest of the
     * functionality in ProxyServerTest. The default implementation creates a new proxy server on port 0 (JVM-assigned port).
     */
    protected ProxyServer createProxyServer() {
        return new ProxyServer(0);
    }

    @After
    public void stopProxyServer() throws Exception {
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            if (proxy != null) {
                proxy.stop();
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
        HttpGet httpGet = new HttpGet(url);

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

    /**
     * Creates an all-trusting CloseableHttpClient (for tests ONLY!) that will connect to a proxy at 127.0.0.1:proxyPort,
     * with no cookie store.
     *
     * @param proxyPort port of the proxy running at 127.0.0.1
     * @return a new CloseableHttpClient
     */
    public static CloseableHttpClient getNewHttpClient(int proxyPort) {
        return getNewHttpClient(proxyPort, null);
    }

    /**
     * Creates an all-trusting CloseableHttpClient (for tests ONLY!) that will connect to a proxy at 127.0.0.1:proxyPort,
     * using the specified cookie store.
     *
     * @param proxyPort port of the proxy running at 127.0.0.1
     * @param cookieStore CookieStore for HTTP cookies
     * @return a new CloseableHttpClient
     */
    public static CloseableHttpClient getNewHttpClient(int proxyPort, CookieStore cookieStore) {
        try {
            // Trust all certs -- under no circumstances should this ever be used outside of testing
            SSLContext sslcontext = SSLContexts.custom()
                    .useTLS()
                    .loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            return true;
                        }
                    })
                    .build();

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslcontext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .setDefaultCookieStore(cookieStore)
                    .setProxy(new HttpHost("127.0.0.1", proxyPort))
                    // disable decompressing content, since some tests want uncompressed content for testing purposes
                    .disableContentCompression()
                    .build();

            return httpclient;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new HTTP client", e);
        }
    }
}
