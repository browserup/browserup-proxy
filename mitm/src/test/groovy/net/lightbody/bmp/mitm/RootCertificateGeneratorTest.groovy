package net.lightbody.bmp.mitm

import net.lightbody.bmp.mitm.test.util.CertificateTestUtil
import net.lightbody.bmp.mitm.keys.RSAKeyGenerator
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.hamcrest.Matchers.greaterThan
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat

class RootCertificateGeneratorTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    CertificateInfo certificateInfo = new CertificateInfo()
            .commonName("littleproxy-test")
            .notAfter(new Date())
            .notBefore(new Date())

    @Test
    void testGenerateRootCertificate() {
        RootCertificateGenerator generator = RootCertificateGenerator.builder()
                .certificateInfo(certificateInfo)
                .keyGenerator(new RSAKeyGenerator())
                .messageDigest("SHA256")
                .build()

        CertificateAndKey certificateAndKey = generator.load()

        CertificateTestUtil.verifyTestRSACertWithCN(certificateAndKey)

        CertificateAndKey secondLoad = generator.load()

        assertEquals("Expected RootCertificateGenerator to return the same instance between calls to .load()", certificateAndKey, secondLoad)
    }

    @Test
    void testCanUseDefaultValues() {
        RootCertificateGenerator generator = RootCertificateGenerator.builder().build()

        CertificateAndKey certificateAndKey = generator.load()

        assertNotNull(certificateAndKey)
    }

    @Test
    void testCanSaveAsPKCS12File() {
        RootCertificateGenerator generator = RootCertificateGenerator.builder().build()

        File file = tmpDir.newFile()

        generator.saveRootCertificateAndKey("PKCS12", file, "privateKey", "password")

        // trivial verification that something was written to the file
        assertThat("Expected file to be >0 bytes after writing certificate and private key", file.length(), greaterThan(0L))
    }

    @Test
    void testCanSaveAsJKSFile() {
        RootCertificateGenerator generator = RootCertificateGenerator.builder().build()

        File file = tmpDir.newFile()

        generator.saveRootCertificateAndKey("JKS", file, "privateKey", "password")

        // trivial verification that something was written to the file
        assertThat("Expected file to be >0 bytes after writing certificate and private key", file.length(), greaterThan(0L))
    }

    @Test
    void testCanEncodeAsPem() {
        RootCertificateGenerator generator = RootCertificateGenerator.builder().build()

        String pemEncodedPrivateKey = generator.encodePrivateKeyAsPem("password")

        // trivial verification that something was written to the string
        assertThat("Expected string containing PEM-encoded private key to contain characters", pemEncodedPrivateKey, not(isEmptyOrNullString()))

        String pemEncodedCertificate = generator.encodeRootCertificateAsPem()
        assertThat("Expected string containing PEM-encoded certificate to contain characters", pemEncodedCertificate , not(isEmptyOrNullString()))
    }

}
