package net.lightbody.bmp.mitm

import net.lightbody.bmp.mitm.exception.ImportException
import net.lightbody.bmp.mitm.test.util.CertificateTestUtil
import net.lightbody.bmp.mitm.util.EncryptionUtil
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import static org.junit.Assert.assertNotNull
import static org.junit.Assume.assumeTrue

class PemFileCertificateSourceTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File certificateFile
    File encryptedPrivateKeyFile
    File unencryptedPrivateKeyFile

    @Before
    void stageFiles() {
        certificateFile = tmpDir.newFile("certificate.crt")
        encryptedPrivateKeyFile = tmpDir.newFile("encrypted-private-key.key")
        unencryptedPrivateKeyFile = tmpDir.newFile("unencrypted-private-key.key")

        Files.copy(KeyStoreFileCertificateSourceTest.getResourceAsStream("/net/lightbody/bmp/mitm/certificate.crt"), certificateFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        Files.copy(KeyStoreFileCertificateSourceTest.getResourceAsStream("/net/lightbody/bmp/mitm/encrypted-private-key.key"), encryptedPrivateKeyFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        Files.copy(KeyStoreFileCertificateSourceTest.getResourceAsStream("/net/lightbody/bmp/mitm/unencrypted-private-key.key"), unencryptedPrivateKeyFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    @Test
    void testCanLoadCertificateAndPasswordProtectedKey() {
        assumeTrue("Skipping test because unlimited strength cryptography is not available", EncryptionUtil.isUnlimitedStrengthAllowed())

        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, encryptedPrivateKeyFile, "password")

        CertificateAndKey certificateAndKey = pemFileCertificateSource.load()
        assertNotNull(certificateAndKey)

        CertificateTestUtil.verifyTestRSACertWithCNandO(certificateAndKey)
    }

    @Test
    void testCanLoadCertificateAndUnencryptedKey() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, unencryptedPrivateKeyFile, null)

        CertificateAndKey certificateAndKey = pemFileCertificateSource.load()
        assertNotNull(certificateAndKey)

        CertificateTestUtil.verifyTestRSACertWithCNandO(certificateAndKey)
    }

    @Test(expected = ImportException)
    void testCannotLoadEncryptedKeyWithoutPassword() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, encryptedPrivateKeyFile, "wrongpassword")

        pemFileCertificateSource.load()
    }

    @Test(expected = ImportException)
    void testIncorrectCertificateFile() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(new File("does-not-exist.crt"), encryptedPrivateKeyFile, "password")

        pemFileCertificateSource.load()
    }

    @Test(expected = IllegalArgumentException)
    void testNullCertificateFile() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(null, encryptedPrivateKeyFile, "password")

        pemFileCertificateSource.load()
    }

    @Test(expected = IllegalArgumentException)
    void testNullPrivateKeyFile() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, null, "password")

        pemFileCertificateSource.load()
    }
}

