package com.browserup.bup.assertion.field.status;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import com.browserup.bup.util.HttpStatusClass;

import java.util.Optional;

public class StatusBelongsToClassAssertion extends StatusPassesPredicateAssertion {
    private final HttpStatusClass statusClass;


    public StatusBelongsToClassAssertion(HttpStatusClass statusClass) {
        this.statusClass = statusClass;
    }

    @Override
    public HarEntryPredicate<Integer> getHarEntryPredicate() {
        return s -> {
            Optional<String> result = Optional.empty();

            if (!statusClass.contains(s)) {
                result = Optional.of(String.format(
                        "Expected response status to belong to class: %s, but was: %d (belongs to %s)",
                        statusClass.name(), s, HttpStatusClass.valueOf(s)
                ));
            }

            return result;
        };
    }
}
