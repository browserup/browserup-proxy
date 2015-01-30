package net.lightbody.bmp.proxy.test.util;

import net.lightbody.bmp.proxy.ProxyServer;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
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
    public void startServer() throws Exception {
        proxy = new ProxyServer(0);
        proxy.start();
        proxyServerPort = proxy.getPort();

        cookieStore = new BasicCookieStore();
        client = getNewHttpClient(proxyServerPort, cookieStore);
    }

    @After
    public void stopServer() throws Exception {
        client.close();
        proxy.stop();
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
                    // disable uncompressing content, since some tests want uncompressed content for testing purposes
                    .disableContentCompression()
                    .build();

            return httpclient;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new HTTP client", e);
        }
    }

}
