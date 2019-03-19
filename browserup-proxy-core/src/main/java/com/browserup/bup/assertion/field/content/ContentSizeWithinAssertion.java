package com.browserup.bup.assertion.field.content;

public class ContentSizeWithinAssertion extends ContentSizePassesPredicateAssertion {

    public ContentSizeWithinAssertion(Long size) {
        super(s -> s != null && s <= size);
    }
}
