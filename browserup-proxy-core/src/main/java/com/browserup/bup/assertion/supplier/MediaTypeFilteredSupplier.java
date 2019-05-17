package com.browserup.bup.assertion.supplier;

import com.browserup.bup.assertion.model.filter.AssertionUrlFilterInfo;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MediaTypeFilteredSupplier extends HarEntriesSupplier {
    private final Pattern mediaType;

    public MediaTypeFilteredSupplier(Har har, Pattern mediaType) {
        super(har, new AssertionUrlFilterInfo());
        this.mediaType = mediaType;
    }

    @Override
    public List<HarEntry> get() {
        return getHar().getLog().getEntries().stream()
                .filter(harEntry -> mediaType.matcher(harEntry.getResponse().getContent().getMimeType()).matches())
                .collect(Collectors.toList());
    }

    public Pattern getMediaType() {
        return mediaType;
    }
}
