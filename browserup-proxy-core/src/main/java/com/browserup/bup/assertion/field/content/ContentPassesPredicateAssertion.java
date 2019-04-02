package com.browserup.bup.assertion.field.content;

import com.browserup.bup.assertion.field.FieldPassesPredicateAssertion;
import com.browserup.bup.assertion.field.HarEntryAssertionFieldSupplier;
import org.apache.commons.lang3.StringUtils;

public abstract class ContentPassesPredicateAssertion extends FieldPassesPredicateAssertion<String> {

    @Override
    public HarEntryAssertionFieldSupplier<String> getFieldSupplier() {
        return entry -> StringUtils.defaultString(entry.getResponse().getContent().getText());
    }
}
