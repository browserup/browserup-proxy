package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class HeadersPassPredicateAssertion extends FieldPassesPredicateAssertion<List<HarHeader>> {
    protected static final Predicate<HarHeader> NONEMPTY_HEADER_FILTER = h -> h.getName() != null && h.getValue() != null;

    public HeadersPassPredicateAssertion(Predicate<List<HarHeader>> predicate) {
        super(entry -> entry.getResponse().getHeaders(), predicate);
    }

    @Override
    public Optional<HarEntryAssertionError> assertion(HarEntry entry) {
        Optional<HarEntryAssertionError> result = super.assertion(entry);
        if (result.isPresent()) {
            result = Optional.of(new HarEntryAssertionError("Headers don't pass predicate"));
        }
        return result;
    }
}
