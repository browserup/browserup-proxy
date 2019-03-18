package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.harreader.model.HarEntry;

import java.util.Optional;
import java.util.function.Predicate;

public class ContentPassesPredicateAssertion extends FieldPassesPredicateAssertion<String> {

    public ContentPassesPredicateAssertion(Predicate<String> predicate) {
        super(entry -> entry.getResponse().getContent().getText(), predicate);
    }

    @Override
    public Optional<HarEntryAssertionError> assertion(HarEntry entry) {
        Optional<HarEntryAssertionError> result = super.assertion(entry);
        if (result.isPresent()) {
            result = Optional.of(new HarEntryAssertionError("Content doesn't pass predicate"));
        }
        return result;
    }
}
