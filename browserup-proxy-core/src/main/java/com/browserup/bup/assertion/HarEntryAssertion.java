package com.browserup.bup.assertion;

import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.harreader.model.HarEntry;

import java.util.Optional;

@FunctionalInterface
public interface HarEntryAssertion {

    Optional<HarEntryAssertionError> assertion(HarEntry entry);

}
