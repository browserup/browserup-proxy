package net.lightbody.bmp.core.har;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class HarLog {
    private final String version = "1.2";
    private volatile HarNameVersion creator;
    private volatile HarNameVersion browser;
    private final List<HarPage> pages = new CopyOnWriteArrayList<HarPage>();
    private final List<HarEntry> entries = new CopyOnWriteArrayList<HarEntry>();
    private volatile String comment = "";

    public HarLog() {
    }

    public HarLog(HarNameVersion creator) {
        this.creator = creator;
    }

    public void addPage(HarPage page) {
        pages.add(page);
    }

    public void addEntry(HarEntry entry) {
        entries.add(entry);
    }

    public String getVersion() {
        return version;
    }

    public HarNameVersion getCreator() {
        return creator;
    }

    public void setCreator(HarNameVersion creator) {
        this.creator = creator;
    }

    public HarNameVersion getBrowser() {
        return browser;
    }

    public void setBrowser(HarNameVersion browser) {
        this.browser = browser;
    }

    public List<HarPage> getPages() {
        return pages;
    }

    public List<HarEntry> getEntries() {
        return entries;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
