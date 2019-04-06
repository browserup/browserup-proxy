/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitm.keys;

import java.security.KeyPair;

/**
 * A functional interface for key pair generators.
 */
public interface KeyGenerator {
    /**
     * Generates a new public/private key pair. This method should not cache or reuse any previously-generated key pairs.
     *
     * @return a new public/private key pair
     */
    KeyPair generate();
}
