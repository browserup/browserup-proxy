package com.browserup.bup.assertion.field.status;

import com.browserup.bup.assertion.field.HarEntryPredicate;

import java.util.Optional;

import static com.browserup.harreader.model.HttpStatus.BAD_REQUEST;

public class SuccessfulStatusAssertion extends StatusPassesPredicateAssertion {
    @Override
    public HarEntryPredicate<Integer> getHarEntryPredicate() {
        return s -> {
            Optional<String> result = Optional.empty();

            if (s >= BAD_REQUEST.getCode()) {
                return Optional.of(String.format(
                        "Expected response status to be less then: '%d', but was: '%d'", BAD_REQUEST.getCode(), s));
            }

            return result;
        };
    }
}
