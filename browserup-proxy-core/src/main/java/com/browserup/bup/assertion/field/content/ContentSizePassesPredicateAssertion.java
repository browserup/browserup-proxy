package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.harreader.model.HarEntry;

import java.util.Optional;
import java.util.function.Predicate;

public class ContentSizePassesPredicateAssertion extends FieldPassesPredicateAssertion<Long> {

    public ContentSizePassesPredicateAssertion(Predicate<Long> predicate) {
        super(entry -> entry.getResponse().getContent().getSize(), predicate);
    }

    @Override
    public Optional<HarEntryAssertionError> assertion(HarEntry entry) {
        Optional<HarEntryAssertionError> result = super.assertion(entry);
        if (result.isPresent()) {
            result = Optional.of(new HarEntryAssertionError("Content size doesn't pass predicate"));
        }
        return result;
    }
}