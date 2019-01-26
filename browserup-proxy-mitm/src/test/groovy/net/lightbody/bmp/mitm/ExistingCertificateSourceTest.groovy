package net.lightbody.bmp.mitm

import org.junit.Test

import java.security.PrivateKey
import java.security.cert.X509Certificate

import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.mock

class ExistingCertificateSourceTest {

    X509Certificate mockCertificate = mock(X509Certificate)
    PrivateKey mockPrivateKey = mock(PrivateKey)

    @Test
    void testLoadExistingCertificateAndKey() {
        ExistingCertificateSource certificateSource = new ExistingCertificateSource(mockCertificate, mockPrivateKey)
        CertificateAndKey certificateAndKey = certificateSource.load()

        assertEquals(mockCertificate, certificateAndKey.certificate)
        assertEquals(mockPrivateKey, certificateAndKey.privateKey)
    }

    @Test(expected = IllegalArgumentException)
    void testMustSupplyCertificate() {
        ExistingCertificateSource certificateSource = new ExistingCertificateSource(null, mockPrivateKey)
        certificateSource.load()
    }

    @Test(expected = IllegalArgumentException)
    void testMustSupplyPrivateKey() {
        ExistingCertificateSource certificateSource = new ExistingCertificateSource(mockCertificate, null)
        certificateSource.load()
    }
}
