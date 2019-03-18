package com.browserup.bup.assertion.field.content;

public class ContentDoesNotContainStringAssertion extends ContentPassesPredicateAssertion {

    public ContentDoesNotContainStringAssertion(String text) {
        super(content -> !content.contains(text));
    }
}
