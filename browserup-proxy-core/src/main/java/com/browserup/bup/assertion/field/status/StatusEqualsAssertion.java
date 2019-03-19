package com.browserup.bup.assertion.field.status;

import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.bup.assertion.field.header.HeadersPassPredicateAssertion;
import com.browserup.harreader.model.HarHeader;

import java.util.List;
import java.util.function.Predicate;

public class StatusEqualsAssertion extends StatusPassesPredicateAssertion {

    public StatusEqualsAssertion(Integer status) {
        super(Predicate.isEqual(status));
    }
}
