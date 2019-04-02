package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.field.HarEntryPredicate;

import java.util.Optional;
import java.util.regex.Pattern;

public class ContentMatchesAssertion extends ContentPassesPredicateAssertion {
    private final Pattern contentPattern;

    public ContentMatchesAssertion(Pattern contentPattern) {
        this.contentPattern = contentPattern;
    }

    @Override
    public HarEntryPredicate<String> getHarEntryPredicate() {
        return content -> {
            Optional<String> result = Optional.empty();
            if (content == null || !contentPattern.matcher(content).matches()) {
                result = Optional.of(
                        String.format("Expected content to match pattern. Pattern: '%s', content: '%s'",
                                contentPattern.pattern(),
                                content
                        ));
            }
            return result;
        };
    }
}
