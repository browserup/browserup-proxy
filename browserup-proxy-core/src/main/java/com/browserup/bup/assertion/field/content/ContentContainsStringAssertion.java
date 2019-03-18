package com.browserup.bup.assertion.field.content;

public class ContentContainsStringAssertion extends ContentPassesPredicateAssertion {

    public ContentContainsStringAssertion(String text) {
        super(content -> content.contains(text));
    }
}
