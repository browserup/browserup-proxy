package com.browserup.bup.assertion.supplier;

import com.browserup.bup.assertion.model.filter.AssertionFilterInfo;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class MostRecentHarEntrySupplier extends HarEntriesSupplier {

    public MostRecentHarEntrySupplier(Har har) {
        super(har, new AssertionFilterInfo());
    }

    @Override
    public List<HarEntry> get() {
        return getHar().getLog().getEntries().stream()
                .max(Comparator.comparing(HarEntry::getStartedDateTime))
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }
}
