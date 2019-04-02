package com.browserup.bup.assertion.field.status;

import com.browserup.bup.assertion.field.HarEntryPredicate;

import java.util.Optional;

public class StatusEqualsAssertion extends StatusPassesPredicateAssertion {
    private final Integer status;

    public StatusEqualsAssertion(Integer status) {
        this.status = status;
    }

    @Override
    public HarEntryPredicate<Integer> getHarEntryPredicate() {
        return s -> {
            Optional<String> result = Optional.empty();

            if (!s.equals(status)) {
                return Optional.of(String.format(
                        "Expected response status to be: '%d', but was: '%d'", status, s));
            }

            return result;
        };
    }
}
