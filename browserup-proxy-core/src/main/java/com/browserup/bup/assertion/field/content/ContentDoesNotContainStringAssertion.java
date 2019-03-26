package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class ContentDoesNotContainStringAssertion extends ContentPassesPredicateAssertion {
    private final String text;

    public ContentDoesNotContainStringAssertion(String text) {
        this.text = text;
    }

    @Override
    public HarEntryPredicate<String> getHarEntryPredicate() {
        return content -> {
            Optional<String> result = Optional.empty();
            if (StringUtils.contains(content, text)) {
                result = Optional.of(
                        String.format(
                                "Expected to find no string with specified value in content. Search string: '%s', content: '%s'",
                                text, content
                        ));
            }
            return result;
        };
    }
}
