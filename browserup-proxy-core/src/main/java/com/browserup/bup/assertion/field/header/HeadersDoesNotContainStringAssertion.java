package com.browserup.bup.assertion.field.header;

public class HeadersDoesNotContainStringAssertion extends HeadersPassPredicateAssertion {

    public HeadersDoesNotContainStringAssertion(String text) {
        super(harHeaders -> harHeaders.stream()
                .anyMatch(h -> !h.getValue().contains(text) && !h.getName().contains(text)));
    }
}
