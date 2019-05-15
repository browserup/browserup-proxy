package com.browserup.bup.assertion.field.status;

import com.browserup.bup.assertion.field.HarEntryPredicate;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

public class NoBrokenStatusAssertion extends StatusPassesPredicateAssertion {
    @Override
    public HarEntryPredicate<Integer> getHarEntryPredicate() {
        return s -> {
            Optional<String> result = Optional.empty();

            if (s >= SC_BAD_REQUEST) {
                return Optional.of(String.format(
                        "Expected response status to be less then: '%d', but was: '%d'", SC_BAD_REQUEST, s));
            }

            return result;
        };
    }
}
