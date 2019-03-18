package com.browserup.bup.assertion.field;

import com.browserup.bup.assertion.HarEntryAssertion;
import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.harreader.model.HarEntry;

import java.util.Optional;
import java.util.function.Predicate;

public class FieldPassesPredicateAssertion<FieldType> implements HarEntryAssertion {
    private final HarEntryAssertionFieldSupplier<FieldType> fieldSupplier;
    private final Predicate<FieldType> predicate;

    public FieldPassesPredicateAssertion(HarEntryAssertionFieldSupplier<FieldType> fieldSupplier, Predicate<FieldType> predicate) {
        this.fieldSupplier = fieldSupplier;
        this.predicate = predicate;
    }

    @Override
    public Optional<HarEntryAssertionError> assertion(HarEntry entry) {
        FieldType input = fieldSupplier.apply(entry);
        if (!predicate.test(input)) {
            return Optional.of(new HarEntryAssertionError("Field doesn't pass predicate"));
        }
        return Optional.empty();
    }
}
