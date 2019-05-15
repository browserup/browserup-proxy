package com.browserup.bup.assertion.supplier;

import com.browserup.bup.assertion.model.filter.AssertionUrlFilterInfo;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;

import java.util.List;
import java.util.stream.Collectors;

public class MimeTypeFilteredSupplier extends HarEntriesSupplier {
    private final String mimeType;

    public MimeTypeFilteredSupplier(Har har, String mimeType) {
        super(har, new AssertionUrlFilterInfo());
        this.mimeType = mimeType;
    }

    @Override
    public List<HarEntry> get() {
        return getHar().getLog().getEntries().stream()
                .filter(harEntry -> harEntry
                        .getResponse()
                        .getContent()
                        .getMimeType()
                        .contains(mimeType)
                )
                .collect(Collectors.toList());
    }

    public String getMimeType() {
        return mimeType;
    }
}
