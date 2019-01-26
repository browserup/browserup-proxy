package net.lightbody.bmp.mitm.tools

import net.lightbody.bmp.mitm.keys.RSAKeyGenerator
import org.junit.Test

import java.security.KeyPair

import static org.junit.Assert.assertNotNull

class RSAKeyGeneratorTest {
    @Test
    void testGenerateWithDefaults() {
        RSAKeyGenerator keyGenerator = new RSAKeyGenerator()
        KeyPair keyPair = keyGenerator.generate()

        assertNotNull(keyPair)
    }

    @Test
    void testGenerateWithExplicitKeySize() {
        RSAKeyGenerator keyGenerator = new RSAKeyGenerator(1024)
        KeyPair keyPair = keyGenerator.generate()

        assertNotNull(keyPair)
        // not much else to verify, other than successful generation
    }
}
