package com.browserup.bup.assertion.field.header;

import com.browserup.harreader.model.HarHeader;

import java.util.regex.Pattern;

public class HeadersMatchAssertion extends HeadersPassPredicateAssertion {

    public HeadersMatchAssertion(Pattern value) {
        super(harHeaders -> harHeaders.stream()
                .filter(NONEMPTY_HEADER_FILTER)
                .map(HarHeader::getValue)
                .anyMatch(hv -> value.matcher(hv).matches()));
    }

    public HeadersMatchAssertion(Pattern name, Pattern value) {
        super(harHeaders -> harHeaders.stream()
                .filter(NONEMPTY_HEADER_FILTER)
                .filter(h -> name.matcher(h.getName()).matches())
                .anyMatch(h -> value.matcher(h.getValue()).matches()));
    }
}
