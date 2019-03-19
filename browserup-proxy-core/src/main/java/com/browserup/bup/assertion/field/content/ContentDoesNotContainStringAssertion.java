package com.browserup.bup.assertion.field.content;

import org.apache.commons.lang3.StringUtils;

public class ContentDoesNotContainStringAssertion extends ContentPassesPredicateAssertion {

    public ContentDoesNotContainStringAssertion(String text) {
        super(content -> !StringUtils.contains(content, text));
    }
}
