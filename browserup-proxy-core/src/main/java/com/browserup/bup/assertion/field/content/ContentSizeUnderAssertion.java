package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.field.HarEntryPredicate;

import java.util.Optional;

public class ContentSizeUnderAssertion extends ContentSizePassesPredicateAssertion {
    private final Long size;

    public ContentSizeUnderAssertion(Long size) {
        this.size = size;
    }

    @Override
    public HarEntryPredicate<Long> getHarEntryPredicate() {
        return s -> {
            Optional<String> result = Optional.empty();
            if (s > size) {
                result = Optional.of(String.format(
                        "Expected content length not to exceed max value. Max value: %d, content length: %d", size, s));
            }
            return result;
        };
    }
}
