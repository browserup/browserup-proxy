package com.browserup.bup.assertion.field.status;

import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.bup.assertion.field.HarEntryAssertionFieldSupplier;

public abstract class StatusPassesPredicateAssertion extends FieldPassesPredicateAssertion<Integer> {

    @Override
    public HarEntryAssertionFieldSupplier<Integer> getFieldSupplier() {
        return entry -> entry.getResponse().getStatus();
    }
}
