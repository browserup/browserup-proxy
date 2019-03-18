package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class HeadersPassPredicateAssertion extends FieldPassesPredicateAssertion<List<HarHeader>> {

    public HeadersPassPredicateAssertion(Predicate<List<HarHeader>> predicate) {
        super(entry -> entry.getResponse().getHeaders(), predicate);
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
