package net.lightbody.bmp.mitm.test.util;

import net.lightbody.bmp.mitm.CertificateAndKey;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Utility methods for X.509 certificate verification in unit tests.
 */
public class CertificateTestUtil {
    /**
     * Asserts that the specified {@link CertificateAndKey} contains an RSA private key and an X.509 certificate
     * with CN="littleproxy-test" and O="LittleProxy test".
     */
    public static void verifyTestRSACertWithCNandO(CertificateAndKey certificateAndKey) {
        X509Certificate certificate = certificateAndKey.getCertificate();
        assertNotNull(certificate);
        assertNotNull(certificate.getIssuerDN());
        assertEquals("CN=littleproxy-test, O=LittleProxy test", certificate.getIssuerDN().getName());

        PrivateKey privateKey = certificateAndKey.getPrivateKey();
        assertNotNull(privateKey);
        assertEquals("RSA", privateKey.getAlgorithm());
    }

    /**
     * Asserts that the specified {@link CertificateAndKey} contains an RSA private key and an X.509 certificate
     * with CN="littleproxy-test".
     */
    public static void verifyTestRSACertWithCN(CertificateAndKey certificateAndKey) {
        X509Certificate certificate = certificateAndKey.getCertificate();
        assertNotNull(certificate);
        assertNotNull(certificate.getIssuerDN());
        assertEquals("CN=littleproxy-test", certificate.getIssuerDN().getName());

        PrivateKey privateKey = certificateAndKey.getPrivateKey();
        assertNotNull(privateKey);
        assertEquals("RSA", privateKey.getAlgorithm());
    }
}
