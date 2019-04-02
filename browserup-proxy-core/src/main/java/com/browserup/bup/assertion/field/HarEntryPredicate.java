package com.browserup.bup.assertion.field;

import java.util.Optional;

public interface HarEntryPredicate<T> {

    Optional<String> test(T entry);
}
