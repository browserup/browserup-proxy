package net.lightbody.bmp.ssl;

import org.littleshoot.proxy.SslEngineSource;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * This implementation mirrors the implementation of {@link org.littleshoot.proxy.extras.SelfSignedSslEngineSource}, but uses the
 * cybervillainsCA.jks keystore that the Jetty implementaion uses.
 */
public class BrowserMobSslEngineSource implements SslEngineSource {
    private static final String KEYSTORE_RESOURCE = "/sslSupport/cybervillainsCA.jks";

    private static final char[] KEYSTORE_PASSWORD = "password".toCharArray();

    private final SSLContext sslContext;

    public BrowserMobSslEngineSource() {
        this.sslContext = initializeSSLContext();
    }

    @Override
    public SSLEngine newSslEngine() {
        return sslContext.createSSLEngine();
    }

    @Override
    public SSLEngine newSslEngine(String host, int port) {
        return sslContext.createSSLEngine(host, port);
    }

    private SSLContext initializeSSLContext() {
        String algorithm = Security
                .getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        InputStream keystoreStream = getClass().getResourceAsStream(KEYSTORE_RESOURCE);
        if (keystoreStream == null) {
            throw new RuntimeException("Unable to load keystore from classpath resource: " + KEYSTORE_RESOURCE);
        }

        try {
            final KeyStore ks = KeyStore.getInstance("JKS");
            // ks.load(new FileInputStream("keystore.jks"),
            // "changeit".toCharArray());
            ks.load(keystoreStream, KEYSTORE_PASSWORD);

            // Set up key manager factory to use our key store
            final KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, KEYSTORE_PASSWORD);

            // Set up a trust manager factory to use our key store
            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(algorithm);
            tmf.init(ks);

            TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
                // TrustManager that trusts all servers
                @Override
                public void checkClientTrusted(X509Certificate[] arg0,
                                               String arg1)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0,
                                               String arg1)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }};

            KeyManager[] keyManagers = kmf.getKeyManagers();

            // Initialize the SSLContext to work with our key managers.
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);

            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize the server-side SSLContext", e);
        }
    }
}
