package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.contains;

public class FilteredHeadersDoNotContainStringAssertion extends HeadersPassPredicateAssertion {
    private final String value;
    private final String name;

    public FilteredHeadersDoNotContainStringAssertion(String name, String value) {
        this.value = value;
        this.name = name;
    }

    @Override
    public HarEntryPredicate<List<HarHeader>> getHarEntryPredicate() {
        return harHeaders -> {
            Optional<HarHeader> found = harHeaders.stream()
                    .filter(NONEMPTY_HEADER_FILTER)
                    .filter(h -> h.getName().equals(name))
                    .filter(h -> contains(h.getValue(), value))
                    .findFirst();

            return found.map(h -> String.format(
                    "Expected to find no header with name '%s' and value containing string '%s'", h.getName(), value));
        };
    }
}
