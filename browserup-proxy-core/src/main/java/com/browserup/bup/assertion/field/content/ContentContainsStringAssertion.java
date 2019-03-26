package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.field.HarEntryPredicate;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class ContentContainsStringAssertion extends ContentPassesPredicateAssertion {
    private final String text;

    public ContentContainsStringAssertion(String text) {
        this.text = text;
    }

    @Override
    public HarEntryPredicate<String> getHarEntryPredicate() {
        return content -> {
            Optional<String> result = Optional.empty();
            if (!StringUtils.contains(content, text)) {
                result = Optional.of(
                        String.format(
                                "Expected to find string in content. Search string: '%s', content: '%s'",
                                text, content
                        ));
            }
            return result;
        };
    }
}
