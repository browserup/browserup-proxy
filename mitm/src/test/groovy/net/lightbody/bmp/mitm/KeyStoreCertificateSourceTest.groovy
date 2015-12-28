package net.lightbody.bmp.mitm

import org.junit.Test

import java.security.KeyStore

import static org.mockito.Mockito.mock

class KeyStoreCertificateSourceTest {

    KeyStore mockKeyStore = mock(KeyStore)

    // the happy-path test cases are already covered implicitly as part of KeyStoreFileCertificateSourceTest, so just test negative cases

    @Test(expected = IllegalArgumentException)
    void testMustSupplyKeystore() {
        KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(null, "privatekey", "password")
        keyStoreCertificateSource.load()
    }

    @Test(expected = IllegalArgumentException)
    void testMustSupplyPassword() {
        KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(mockKeyStore, "privatekey", null)
        keyStoreCertificateSource.load()
    }

    @Test(expected = IllegalArgumentException)
    void testMustSupplyPrivateKeyAlias() {
        KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(mockKeyStore, null, "password")
        keyStoreCertificateSource.load()
    }

}
