package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.bup.assertion.field.HarEntryAssertionFieldSupplier;

import java.util.Optional;

public abstract class ContentSizePassesPredicateAssertion extends FieldPassesPredicateAssertion<Long> {

    @Override
    public HarEntryAssertionFieldSupplier<Long> getFieldSupplier() {
        return entry -> Optional.ofNullable(entry.getResponse().getContent().getSize()).orElse(0L);
    }
}