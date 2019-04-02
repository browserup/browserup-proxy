package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.contains;

public class FilteredHeadersContainStringAssertion extends HeadersPassPredicateAssertion {
    private final String name;
    private final String value;

    public FilteredHeadersContainStringAssertion(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public HarEntryPredicate<List<HarHeader>> getHarEntryPredicate() {
        return harHeaders -> {
            Optional<String> result = Optional.empty();
            boolean contains = harHeaders.stream()
                    .filter(NONEMPTY_HEADER_FILTER)
                    .filter(h -> h.getName().equals(name))
                    .anyMatch(h -> contains(h.getValue(), value));
            if (!contains) {
                result = Optional.of(String.format(
                        "Expected to find header with name: '%s' and value containing string: '%s'", name, value
                ));
            }
            return result;
        };
    }
}
