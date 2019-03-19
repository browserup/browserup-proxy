package com.browserup.bup.assertion.field.content;

import org.apache.commons.lang3.StringUtils;

public class ContentContainsStringAssertion extends ContentPassesPredicateAssertion {

    public ContentContainsStringAssertion(String text) {
        super(content -> StringUtils.contains(content, text));
    }
}
