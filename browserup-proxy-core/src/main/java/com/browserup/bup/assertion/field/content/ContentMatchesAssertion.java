package com.browserup.bup.assertion.field.content;

import java.util.regex.Pattern;

public class ContentMatchesAssertion extends ContentPassesPredicateAssertion {

    public ContentMatchesAssertion(Pattern contentPattern) {
        super(content -> content != null && contentPattern.matcher(content).matches());
    }
}
