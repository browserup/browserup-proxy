package net.lightbody.bmp.mitm.tools

import net.lightbody.bmp.mitm.keys.ECKeyGenerator
import org.junit.Test

import java.security.KeyPair

import static org.junit.Assert.assertNotNull

class ECKeyGeneratorTest {
    @Test
    void testGenerateWithDefaults() {
        ECKeyGenerator keyGenerator = new ECKeyGenerator()
        KeyPair keyPair = keyGenerator.generate()

        assertNotNull(keyPair)
    }

    @Test
    void testGenerateWithExplicitNamedCurve() {
        ECKeyGenerator keyGenerator = new ECKeyGenerator("secp384r1")
        KeyPair keyPair = keyGenerator.generate()

        assertNotNull(keyPair)
        // not much else to verify, other than successful generation
    }
}
