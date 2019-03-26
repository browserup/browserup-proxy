package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.bup.assertion.field.HarEntryAssertionFieldSupplier;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.function.Predicate;

public abstract class HeadersPassPredicateAssertion extends FieldPassesPredicateAssertion<List<HarHeader>> {
    protected static final Predicate<HarHeader> NONEMPTY_HEADER_FILTER = h -> h.getName() != null && h.getValue() != null;

    @Override
    public HarEntryAssertionFieldSupplier<List<HarHeader>> getFieldSupplier() {
        return entry -> entry.getResponse().getHeaders();
    }
}
