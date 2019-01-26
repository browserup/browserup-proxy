package net.lightbody.bmp.mitm

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.security.cert.X509Certificate

import static org.hamcrest.Matchers.both
import static org.hamcrest.Matchers.emptyArray
import static org.hamcrest.Matchers.not
import static org.hamcrest.Matchers.notNullValue
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat

class TrustSourceTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File certificateFile

    @Before
    void stageFiles() {
        certificateFile = tmpDir.newFile("certificate.crt")

        Files.copy(KeyStoreFileCertificateSourceTest.getResourceAsStream("/net/lightbody/bmp/mitm/certificate.crt"), certificateFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    @Test
    void testLoadJavaTrustSource() {
        TrustSource trustSource = TrustSource.javaTrustSource()
        X509Certificate[] trustedCAs = trustSource.getTrustedCAs()

        assertThat("Expected default Java trust source to contain some trusted CAs", trustedCAs,
                both(notNullValue()).and(not(emptyArray())))
    }

    @Test
    void testLoadBuiltinTrustSource() {
        TrustSource trustSource = TrustSource.builtinTrustSource()
        X509Certificate[] trustedCAs = trustSource.getTrustedCAs()

        assertThat("Expected default Java trust source to contain some trusted CAs", trustedCAs,
                both(notNullValue()).and(not(emptyArray())))
    }

    @Test
    void testCanAddCertificateToJavaTrustSource() {
        TrustSource trustSource = TrustSource.javaTrustSource()
        int trustedCACount = trustSource.getTrustedCAs().length

        TrustSource newTrustSource = trustSource.add("-----BEGIN CERTIFICATE-----\n" +
                "MIIDdTCCAl2gAwIBAgILBAAAAAABFUtaw5QwDQYJKoZIhvcNAQEFBQAwVzELMAkGA1UEBhMCQkUx\n" +
                "GTAXBgNVBAoTEEdsb2JhbFNpZ24gbnYtc2ExEDAOBgNVBAsTB1Jvb3QgQ0ExGzAZBgNVBAMTEkds\n" +
                "b2JhbFNpZ24gUm9vdCBDQTAeFw05ODA5MDExMjAwMDBaFw0yODAxMjgxMjAwMDBaMFcxCzAJBgNV\n" +
                "BAYTAkJFMRkwFwYDVQQKExBHbG9iYWxTaWduIG52LXNhMRAwDgYDVQQLEwdSb290IENBMRswGQYD\n" +
                "VQQDExJHbG9iYWxTaWduIFJvb3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDa\n" +
                "DuaZjc6j40+Kfvvxi4Mla+pIH/EqsLmVEQS98GPR4mdmzxzdzxtIK+6NiY6arymAZavpxy0Sy6sc\n" +
                "THAHoT0KMM0VjU/43dSMUBUc71DuxC73/OlS8pF94G3VNTCOXkNz8kHp1Wrjsok6Vjk4bwY8iGlb\n" +
                "Kk3Fp1S4bInMm/k8yuX9ifUSPJJ4ltbcdG6TRGHRjcdGsnUOhugZitVtbNV4FpWi6cgKOOvyJBNP\n" +
                "c1STE4U6G7weNLWLBYy5d4ux2x8gkasJU26Qzns3dLlwR5EiUWMWea6xrkEmCMgZK9FGqkjWZCrX\n" +
                "gzT/LCrBbBlDSgeF59N89iFo7+ryUp9/k5DPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV\n" +
                "HRMBAf8EBTADAQH/MB0GA1UdDgQWBBRge2YaRQ2XyolQL30EzTSo//z9SzANBgkqhkiG9w0BAQUF\n" +
                "AAOCAQEA1nPnfE920I2/7LqivjTFKDK1fPxsnCwrvQmeU79rXqoRSLblCKOzyj1hTdNGCbM+w6Dj\n" +
                "Y1Ub8rrvrTnhQ7k4o+YviiY776BQVvnGCv04zcQLcFGUl5gE38NflNUVyRRBnMRddWQVDf9VMOyG\n" +
                "j/8N7yy5Y0b2qvzfvGn9LhJIZJrglfCm7ymPAbEVtQwdpf5pLGkkeB6zpxxxYu7KyJesF12KwvhH\n" +
                "hm4qxFYxldBniYUr+WymXUadDKqC5JlR3XC321Y9YeRq4VzW9v493kHMB65jUr9TU/Qr6cf9tveC\n" +
                "X4XSQRjbgbMEHMUfpIBvFSDJ3gyICh3WZlXi/EjJKSZp4A==\n" +
                "-----END CERTIFICATE-----")

        int newTrustedCACount = newTrustSource.getTrustedCAs().length

        assertEquals("Expected trust source with additional CA to be larger than original trust source", trustedCACount + 1, newTrustedCACount)
    }

    @Test
    void testCanAddTrustedCertificateInFile() {
        TrustSource trustSource = TrustSource.javaTrustSource()
        int trustedCACount = trustSource.getTrustedCAs().length

        TrustSource newTrustSource = trustSource.add(certificateFile)
        int newTrustedCACount = newTrustSource.getTrustedCAs().length

        assertEquals("Expected trust source with additional CA to be larger than original trust source", trustedCACount + 1, newTrustedCACount)
    }

    @Test
    void testCanAddTrustedCertificateInKeyStore() {
        InputStream keystoreStream = TrustSource.class.getResourceAsStream("/net/lightbody/bmp/mitm/trusted-cert.jks")
        assertNotNull("Unable to load keystore", keystoreStream)
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(keystoreStream, null)

        TrustSource trustSource = TrustSource.javaTrustSource()
        int trustedCACount = trustSource.getTrustedCAs().length

        TrustSource newTrustSource = trustSource.add(keyStore)
        int newTrustedCACount = newTrustSource.getTrustedCAs().length

        assertEquals("Expected trust source with additional CA to be larger than original trust source", trustedCACount + 1, newTrustedCACount)
    }
}
