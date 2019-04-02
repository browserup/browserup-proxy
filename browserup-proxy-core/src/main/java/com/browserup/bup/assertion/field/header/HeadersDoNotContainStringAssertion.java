package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.contains;

public class HeadersDoNotContainStringAssertion extends HeadersPassPredicateAssertion {
    private final String value;

    public HeadersDoNotContainStringAssertion(String value) {
        this.value = value;
    }

    @Override
    public HarEntryPredicate<List<HarHeader>> getHarEntryPredicate() {
        return harHeaders -> {
            Optional<HarHeader> found = harHeaders.stream()
                    .filter(NONEMPTY_HEADER_FILTER)
                    .filter(hv -> contains(hv.getValue(), value))
                    .findFirst();

            return found.map(h -> String.format(
                    "Expected to find no headers containing string '%s'. Found header with name: '%s' containing string: '%s'",
                    value, h.getName(), value
            ));
        };
    }
}
