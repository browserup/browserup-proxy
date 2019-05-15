package com.browserup.bup.assertion.supplier;

import com.browserup.bup.assertion.model.filter.AssertionUrlFilterInfo;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MediaTypeFilteredSupplier extends HarEntriesSupplier {
    private final String mediaTypeRegex;

    public MediaTypeFilteredSupplier(Har har, String mediaTypeRegex) {
        super(har, new AssertionUrlFilterInfo());
        this.mediaTypeRegex = mediaTypeRegex;
    }

    @Override
    public List<HarEntry> get() {
        return getHar().getLog().getEntries().stream()
                .filter(harEntry -> Pattern.matches(mediaTypeRegex, harEntry.getResponse().getContent().getMimeType()))
                .collect(Collectors.toList());
    }

    public String getMediaTypeRegex() {
        return mediaTypeRegex;
    }
}
