package com.browserup.bup.assertion.field;

import com.browserup.bup.assertion.HarEntryAssertion;
import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.harreader.model.HarEntry;

import java.util.Optional;

public abstract class FieldPassesPredicateAssertion<FieldType> implements HarEntryAssertion {

    public abstract HarEntryAssertionFieldSupplier<FieldType> getFieldSupplier();

    public abstract HarEntryPredicate<FieldType> getHarEntryPredicate();

    @Override
    public Optional<HarEntryAssertionError> assertion(HarEntry entry) {
        FieldType input = getFieldSupplier().apply(entry);

        return getHarEntryPredicate().test(input).map(HarEntryAssertionError::new);
    }
}
