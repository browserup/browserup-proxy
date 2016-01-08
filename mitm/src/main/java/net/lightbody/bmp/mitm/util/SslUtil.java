package net.lightbody.bmp.mitm.util;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import net.lightbody.bmp.mitm.exception.SslContextInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Utility for creating SSLContexts.
 */
public class SslUtil {
    private static final Logger log = LoggerFactory.getLogger(SslUtil.class);

    /**
     * Creates a netty SslContext for use when connecting to upstream servers. When trustAllServers is true, no upstream certificate
     * verification will be performed. <b>This will make it possible for attackers to MITM communications with the upstream
     * server</b>, so use trustAllServers only when testing.
     *
     * @param trustAllServers when true, no upstream server certificate validation will be performed
     * @return an SSLContext to connect to upstream servers with
     */
    public static SslContext getUpstreamServerSslContext(boolean trustAllServers) {
        //TODO: add the ability to specify an explicit additional trust source, so clients don't need to import trust into the JDK trust source or forgo trust entirely

        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

        if (trustAllServers) {
            log.warn("Disabling upstream server certificate verification. This will allow attackers to intercept communications with upstream servers.");

            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }

        try {
            return sslContextBuilder.build();
        } catch (SSLException e) {
            throw new SslContextInitializationException("Error creating new SSL context for connection to upstream server", e);
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
