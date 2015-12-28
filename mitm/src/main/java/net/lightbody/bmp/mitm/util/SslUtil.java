package net.lightbody.bmp.mitm.util;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import net.lightbody.bmp.mitm.exception.SslContextInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Utility for creating SSLContexts.
 */
public class SslUtil {
    private static final Logger log = LoggerFactory.getLogger(SslUtil.class);

    /**
     * Creates an SSLContext for use when connecting to upstream servers. When trustAllServers is true, no upstream certificate
     * verification will be performed. <b>This will make it possible for attackers to MITM communications with the upstream
     * server</b>, so use trustAllServers only when testing.
     *
     * @param trustAllServers when true, no upstream server certificate validation will be performed
     * @return an SSLContext to connect to upstream servers with
     */
    public static SSLContext getUpstreamServerSslContext(boolean trustAllServers) {
        //TODO: add the ability to specify an explicit additional trust source, so clients don't need to import trust into the JDK trust source or forgo trust entirely

        try {
            if (trustAllServers) {
                log.warn("Disabling upstream server certificate verification. This will allow attackers to intercept communications with upstream servers.");

                TrustManager[] trustManagers = InsecureTrustManagerFactory.INSTANCE.getTrustManagers();

                // start with the default SSL context, but override the default TrustManager with the "always trust everything" TrustManager
                SSLContext newSslContext = SSLContext.getInstance("TLS");
                newSslContext.init(null, trustManagers, null);

                return newSslContext;
            } else {
                return SSLContext.getDefault();
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new SslContextInitializationException("Error creating new SSL context for connection to upstream server", e);
        }
    }

    /**
     * Creates an SSLContext for use with clients' connections to this server. The specified keyManagers should contain
     * the impersonated server certificate and private key used to encrypt communications with the client.
     *
     * @param keyManagers keyManagers that will be used to encrypt communications with the client; should contain the impersonated upstream server certificate
     * @return SSLContext for use with clients' connections to this server
     */
    public static SSLContext getClientSslContext(KeyManager[] keyManagers) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, null, null);

            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new SslContextInitializationException("Error creating new SSL context for connection to client", e);
        }


    }

    /**
     * Returns the X509Certificate for the server this session is connected to. The certificate may be null.
     *
     * @param sslSession SSL session connected to upstream server
     * @return the X.509 certificate from the upstream server, or null if no certificate is available
     */
    public static X509Certificate getServerCertificate(SSLSession sslSession) {
        Certificate[] peerCertificates;
        try {
            peerCertificates = sslSession.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            peerCertificates = null;
        }

        if (peerCertificates != null && peerCertificates.length > 0) {
            Certificate peerCertificate = peerCertificates[0];
            if (peerCertificate != null && peerCertificate instanceof X509Certificate) {
                return (X509Certificate) peerCertificates[0];
            }
        }

        // no X.509 certificate was found for this server
        return null;
    }
}
