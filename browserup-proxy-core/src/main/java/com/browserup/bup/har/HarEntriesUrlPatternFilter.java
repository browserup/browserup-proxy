package com.browserup.bup.har;

import com.browserup.harreader.model.HarEntry;

import java.util.regex.Pattern;

public class HarEntriesUrlPatternFilter implements HarEntriesFilter {

    private final Pattern pattern;

    public HarEntriesUrlPatternFilter(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public boolean test(HarEntry entry) {
        return pattern.matcher(entry.getRequest().getUrl()).matches();
    }
}
