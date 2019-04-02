package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.bup.assertion.field.HarEntryAssertionFieldSupplier;

public abstract class ContentSizePassesPredicateAssertion extends FieldPassesPredicateAssertion<Long> {

    @Override
    public HarEntryAssertionFieldSupplier<Long> getFieldSupplier() {
        return entry -> entry.getResponse().getContent().getSize();
    }
}