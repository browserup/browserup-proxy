package net.lightbody.bmp.proxy.test.util;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A base class that spins up and shuts down a BrowserMobProxy instance using the new interface. IT also provides mock server support via
 * {@link net.lightbody.bmp.proxy.test.util.MockServerTest}.
 */
public class NewProxyServerTest extends MockServerTest {
    protected BrowserMobProxy proxy;

    @Before
    public void setUpProxyServer() {
        proxy = new BrowserMobProxyServer();
        proxy.start();
    }

    @After
    public void shutDownProxyServer() {
        if (proxy != null) {
            proxy.abort();
        }
    }

}
