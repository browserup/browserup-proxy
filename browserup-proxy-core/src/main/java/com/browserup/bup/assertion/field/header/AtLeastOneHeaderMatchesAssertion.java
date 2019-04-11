package com.browserup.bup.assertion.field.header;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AtLeastOneHeaderMatchesAssertion extends HeadersPassPredicateAssertion {
    private final Pattern valuePattern;

    public AtLeastOneHeaderMatchesAssertion(Pattern valuePattern) {
        this.valuePattern = valuePattern;
    }

    @Override
    public HarEntryPredicate<List<HarHeader>> getHarEntryPredicate() {
        return harHeaders -> {
            Optional<String> result = Optional.empty();

            boolean foundMatched = harHeaders.stream()
                    .filter(NONEMPTY_HEADER_FILTER)
                    .anyMatch(h -> valuePattern.matcher(h.getValue()).matches());

            if (!foundMatched) {
                result = Optional.of("Expected to find at least one header matching value pattern");
            }

            return result;
        };
    }
}
