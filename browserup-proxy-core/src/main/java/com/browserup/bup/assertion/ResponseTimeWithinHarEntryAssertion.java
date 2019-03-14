package com.browserup.bup.assertion;

import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.harreader.model.HarEntry;

import java.util.Optional;

public class ResponseTimeWithinHarEntryAssertion implements HarEntryAssertion {
    private final Long time;

    public ResponseTimeWithinHarEntryAssertion(Long time) {
        this.time = time;
    }

    @Override
    public Optional<HarEntryAssertionError> assertion(HarEntry entry) {
        if (entry.getTime() > time) {
            return Optional.of(new HarEntryAssertionError("Time exceeded", time, entry.getTime()));
        }
        return Optional.empty();
    }
}
