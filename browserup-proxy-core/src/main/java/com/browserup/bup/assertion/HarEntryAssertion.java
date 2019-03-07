package com.browserup.bup.assertion;

import com.browserup.bup.exception.AssertionException;
import com.browserup.harreader.model.HarEntry;

@FunctionalInterface
public interface HarEntryAssertion {

    void assertion(HarEntry entry) throws AssertionException;
}
