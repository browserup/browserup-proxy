package com.browserup.harreader.filter;

import com.browserup.harreader.model.HarEntry;
import java.util.function.Predicate;

public interface HarEntriesFilter extends Predicate<HarEntry> {

    @Override
    boolean test(HarEntry entry);
}
