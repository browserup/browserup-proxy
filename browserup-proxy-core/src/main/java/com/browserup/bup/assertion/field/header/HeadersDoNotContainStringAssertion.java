package com.browserup.bup.assertion.field.header;

import com.browserup.harreader.model.HarHeader;

import static org.apache.commons.lang3.StringUtils.contains;

public class HeadersDoNotContainStringAssertion extends HeadersPassPredicateAssertion {

    public HeadersDoNotContainStringAssertion(String value) {
        super(harHeaders -> harHeaders.stream()
                .filter(NONEMPTY_HEADER_FILTER)
                .map(HarHeader::getValue)
                .noneMatch(hv -> contains(hv, value)));
    }

    public HeadersDoNotContainStringAssertion(String name, String value) {
        super(harHeaders -> harHeaders.stream()
                .filter(NONEMPTY_HEADER_FILTER)
                .filter(h -> h.getName().equals(name))
                .noneMatch(h -> contains(h.getValue(), value)));
    }
}
