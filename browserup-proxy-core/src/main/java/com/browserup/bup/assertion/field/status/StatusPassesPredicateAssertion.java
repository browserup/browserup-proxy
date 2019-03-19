package com.browserup.bup.assertion.field.status;

import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.harreader.model.HarEntry;

import java.util.Optional;
import java.util.function.Predicate;

public class StatusPassesPredicateAssertion extends FieldPassesPredicateAssertion<Integer> {

    public StatusPassesPredicateAssertion(Predicate<Integer> predicate) {
        super(entry -> entry.getResponse().getStatus(), predicate);
    }

    @Override
    public Optional<HarEntryAssertionError> assertion(HarEntry entry) {
        Optional<HarEntryAssertionError> result = super.assertion(entry);
        if (result.isPresent()) {
            result = Optional.of(new HarEntryAssertionError("Status doesn't pass predicate"));
        }
        return result;
    }
}
