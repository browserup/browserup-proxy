package com.browserup.bup.assertion.field.header;

import org.apache.commons.lang3.StringUtils;

public class HeadersDoesNotContainStringAssertion extends HeadersPassPredicateAssertion {

    public HeadersDoesNotContainStringAssertion(String text) {
        super(harHeaders -> harHeaders.stream()
                .allMatch(h ->
                        !StringUtils.contains(h.getValue(), text)
                                && !StringUtils.contains(h.getName(), text)
                ));
    }
}
