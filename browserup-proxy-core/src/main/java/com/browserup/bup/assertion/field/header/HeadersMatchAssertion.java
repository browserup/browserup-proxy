package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HeadersMatchAssertion extends HeadersPassPredicateAssertion {
    private final Pattern valuePattern;

    public HeadersMatchAssertion(Pattern valuePattern) {
        this.valuePattern = valuePattern;
    }

    @Override
    public HarEntryPredicate<List<HarHeader>> getHarEntryPredicate() {
        return harHeaders -> {
            Optional<String> result = Optional.empty();

            List<HarHeader> notMatchingHeaders = harHeaders.stream()
                    .filter(NONEMPTY_HEADER_FILTER)
                    .filter(h -> !valuePattern.matcher(h.getValue()).matches())
                    .collect(Collectors.toList());

            if (notMatchingHeaders.size() > 0) {
                String notMatchingHeadersNames = notMatchingHeaders.stream()
                        .map(HarHeader::getName)
                        .collect(Collectors.joining(","));

                result = Optional.of(String.format(
                        "Expected headers values to match pattern: '%s'. Headers names which values don't match value pattern: %s",
                        valuePattern.pattern(), notMatchingHeadersNames
                ));
            }

            return result;
        };
    }
}
