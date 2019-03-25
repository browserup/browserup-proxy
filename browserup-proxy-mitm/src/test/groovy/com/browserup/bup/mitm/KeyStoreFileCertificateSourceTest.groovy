/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitm

import com.browserup.bup.mitm.test.util.CertificateTestUtil
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Files
import java.nio.file.StandardCopyOption

class KeyStoreFileCertificateSourceTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File pkcs12File
    File jksFile

    @Before
    void stageFiles() {
        pkcs12File = tmpDir.newFile("keystore.p12")
        jksFile = tmpDir.newFile("keystore.jks")

        Files.copy(KeyStoreFileCertificateSourceTest.getResourceAsStream("/net/lightbody/bup/mitm/keystore.p12"), pkcs12File.toPath(), StandardCopyOption.REPLACE_EXISTING)
        Files.copy(KeyStoreFileCertificateSourceTest.getResourceAsStream("/net/lightbody/bup/mitm/keystore.jks"), jksFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    @Test
    void testPkcs12FileOnClasspath() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("PKCS12", "/net/lightbody/bup/mitm/keystore.p12", "privateKey", "password")

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load()

        CertificateTestUtil.verifyTestRSACertWithCNandO(certificateAndKey)
    }

    @Test
    void testPkcs12FileOnDisk() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("PKCS12", pkcs12File, "privateKey", "password")

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load()

        CertificateTestUtil.verifyTestRSACertWithCNandO(certificateAndKey)
    }

    @Test
    void testJksFileOnClasspath() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("JKS", "/net/lightbody/bup/mitm/keystore.jks", "privateKey", "password")

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load()

        CertificateTestUtil.verifyTestRSACertWithCNandO(certificateAndKey)
    }

    @Test
    void testJksFileOnDisk() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("JKS", jksFile, "privateKey", "password")

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load()

        CertificateTestUtil.verifyTestRSACertWithCNandO(certificateAndKey)
    }
}
