package com.browserup.harreader.filter;

import com.browserup.harreader.model.HarEntry;
import java.util.regex.Pattern;

public class HarEntriesUrlPatternFilter implements HarEntriesFilter {

    private final Pattern pattern;

    public HarEntriesUrlPatternFilter(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean test(HarEntry entry) {
        return pattern.matcher(entry.getRequest().getUrl()).matches();
    }
}
