package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.contains;

public class HeadersContainStringAssertion extends HeadersPassPredicateAssertion {
    private final String value;

    public HeadersContainStringAssertion(String value) {
        this.value = value;
    }

    @Override
    public HarEntryPredicate<List<HarHeader>> getHarEntryPredicate() {
        return harHeaders -> {
            Optional<String> result = Optional.empty();
            boolean contains = harHeaders.stream()
                    .filter(NONEMPTY_HEADER_FILTER)
                    .map(HarHeader::getValue)
                    .anyMatch(hv -> contains(hv, value));
            if (!contains) {
                result = Optional.of(String.format(
                        "Expected to find one or more headers containing string: '%s'", value
                ));
            }
            return result;
        };
    }

}
