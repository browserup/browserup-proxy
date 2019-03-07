package com.browserup.bup.assertion;

import com.browserup.bup.exception.AssertionException;
import com.browserup.harreader.model.HarEntry;

public class ResponseTimeWithinHarEntryAssertion implements HarEntryAssertion {
    private final Long time;

    public ResponseTimeWithinHarEntryAssertion(Long time) {
        this.time = time;
    }

    @Override
    public void assertion(HarEntry entry) throws AssertionException {
        if (entry.getTime() > time) {
            throw new AssertionException(String.format("Time exceeded, expected: %d, actual: %d", time, entry.getTime()));
        }
    }
}
