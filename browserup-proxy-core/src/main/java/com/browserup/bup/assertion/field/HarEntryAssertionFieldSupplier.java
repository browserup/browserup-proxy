package com.browserup.bup.assertion.field;

import com.browserup.harreader.model.HarEntry;

import java.util.function.Function;

@FunctionalInterface
public interface HarEntryAssertionFieldSupplier<FieldType> extends Function<HarEntry, FieldType> {
}
