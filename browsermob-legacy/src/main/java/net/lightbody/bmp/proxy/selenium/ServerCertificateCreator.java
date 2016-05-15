package net.lightbody.bmp.proxy.selenium;

import net.lightbody.bmp.mitm.CertificateAndKey;
import net.lightbody.bmp.mitm.CertificateInfo;
import net.lightbody.bmp.mitm.CertificateInfoGenerator;
import net.lightbody.bmp.mitm.HostnameCertificateInfoGenerator;
import net.lightbody.bmp.mitm.tools.DefaultSecurityProviderTool;
import net.lightbody.bmp.mitm.tools.SecurityProviderTool;
import net.lightbody.bmp.mitm.util.MitmConstants;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * Utility to create server certificates for legacy {@link net.lightbody.bmp.proxy.ProxyServer} MITM.
 */
public class ServerCertificateCreator {
    /**
     * Use the default hostname-impersonating certificate info generator that the MITM module provides.
     */
    private static final CertificateInfoGenerator CERT_INFO_GENERATOR = new HostnameCertificateInfoGenerator();

    /**
     * Use the default (JDK where available, otherwise BC) security provider to generate certificates.
     */
    private static final SecurityProviderTool SECURITY_PROVIDER = new DefaultSecurityProviderTool();

    public static X509Certificate generateStdSSLServerCertificate(
            KeyPair newPublicAndPrivateKey,
            X509Certificate caCert,
            PrivateKey caPrivateKey,
            String hostname) {
        CertificateInfo certificateInfo = CERT_INFO_GENERATOR.generate(Collections.singletonList(hostname), null);

        CertificateAndKey newServerCert = SECURITY_PROVIDER.createServerCertificate(
                certificateInfo,
                caCert,
                caPrivateKey,
                newPublicAndPrivateKey,
                MitmConstants.DEFAULT_MESSAGE_DIGEST);

        return newServerCert.getCertificate();
    }
}
