package com.browserup.bup.assertion.field.header;

public class HeadersContainStringAssertion extends HeadersPassPredicateAssertion {

    public HeadersContainStringAssertion(String text) {
        super(harHeaders -> harHeaders.stream()
                .anyMatch(h -> h.getValue().contains(text) || h.getName().contains(text)));
    }
}
