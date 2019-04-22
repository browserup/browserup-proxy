package com.browserup.harreader.model;

import com.browserup.harreader.filter.HarEntriesFilter;
import com.browserup.harreader.filter.HarEntriesUrlPatternFilter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Root object of exported data.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#log">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarLog {

    protected static final String DEFAULT_VERSION = "1.1";

    private String version = DEFAULT_VERSION;
    private HarCreatorBrowser creator;
    private HarCreatorBrowser browser;
    private List<HarPage> pages = new ArrayList<>();
    private List<HarEntry> entries = new ArrayList<>();
    private String comment;

    /**
     * @return Version number of the format.
     * Defaults to {@link #DEFAULT_VERSION}
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version == null || version.trim().equals("")) {
            version = DEFAULT_VERSION;
        }
        this.version = version;
    }

    /**
     * @return Information about the application used to generate HAR.
     */
    public HarCreatorBrowser getCreator() {
        if (creator == null) {
            creator = new HarCreatorBrowser();
        }
        return creator;
    }

    public void setCreator(HarCreatorBrowser creator) {
        this.creator = creator;
    }

    /**
     * @return Information about the browser used.
     */
    public HarCreatorBrowser getBrowser() {
        if (browser == null) {
            browser = new HarCreatorBrowser();
        }
        return browser;
    }

    public void setBrowser(HarCreatorBrowser browser) {
        this.browser = browser;
    }

    /**
     * @return List of all exported pages, may be empty.
     */
    public List<HarPage> getPages() {
        if (pages == null) {
            pages = new ArrayList<>();
        }
        return pages;
    }

    public void setPages(List<HarPage> pages) {
        this.pages = pages;
    }

    /**
     * @return List of all exported requests, may be empty.
     */
    public List<HarEntry> getEntries() {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        return entries;
    }

    public void setEntries(List<HarEntry> entries) {
        this.entries = entries;
    }

    /**
     * @return Comment provided by the user or application, null if not present.
     */
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HarLog harLog = (HarLog) o;
        return Objects.equals(version, harLog.version) &&
                Objects.equals(creator, harLog.creator) &&
                Objects.equals(browser, harLog.browser) &&
                Objects.equals(pages, harLog.pages) &&
                Objects.equals(entries, harLog.entries) &&
                Objects.equals(comment, harLog.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, creator, browser, pages, entries, comment);
    }

    /**
     * Search the entire log for the most recent entry whose request URL matches the given <code>url</code>.
     *
     * @param url Regular expression match of URL to find.
     *            URLs are formatted as: scheme://host:port/path?querystring.
     *            Port is not included in the URL if it is the standard port for the scheme.
     *            Fragments (example.com/#fragment) should not be included in the URL.
     *            If more than one URL found, return the most recently requested URL.
     *            Pattern examples:
     *            - Match a URL with "http" or "https" protocol, "example.com" domain, and "/index.html" exact file path, with no query parameters:
     *              "^(http|https)://example\\.com/index\\.html$"
     *            - Match a URL with "http" protocol, "example.com" domain, "/customer" exact path, followed by any query string:
     *              "^http://example\\.com/customer\\?.*"
     *            - Match a URL with "http" protocol, "example.com" domain, "/products" path, and exactly 1 UUID query parameter named "id":
     *              "^http://example\\.com/products\\?id=[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}$"
     * @return <code>HarEntry</code> for the most recently requested URL matching the given <code>url</code> pattern.
     */
    public Optional<HarEntry> findMostRecentEntry(Pattern url) {
        return findEntries(new HarEntriesUrlPatternFilter(url)).stream()
            .max(Comparator.comparing(HarEntry::getStartedDateTime));
    }

    /**
     * Search the entire log for entries whose request URL matches the given <code>url</code>.
     *
     * @param url Regular expression match of URL to find.
     *            URLs are formatted as: scheme://host:port/path?querystring.
     *            Port is not included in the URL if it is the standard port for the scheme.
     *            Fragments (example.com/#fragment) should not be included in the URL.
     *            If more than one URL found, use the most recently requested URL.
     *            Pattern examples:
     *            - Match a URL with "http" or "https" protocol, "example.com" domain, and "/index.html" exact file path, with no query parameters:
     *              "^(http|https)://example\\.com/index\\.html$"
     *            - Match a URL with "http" protocol, "example.com" domain, "/customer" exact path, followed by any query string:
     *              "^http://example\\.com/customer\\?.*"
     *            - Match a URL with "http" protocol, "example.com" domain, "/products" path, and exactly 1 UUID query parameter named "id":
     *              "^http://example\\.com/products\\?id=[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}$"
     *
     * @return A list of <code>HarEntry</code> for any requests whose URL matches the given <code>url</code> pattern,
     *         or an empty list if none match.
     */
    public List<HarEntry> findEntries(Pattern url) {
        return findEntries(new HarEntriesUrlPatternFilter(url));
    }

    private List<HarEntry> findEntries(HarEntriesFilter filter) {
        return getEntries().stream()
            .filter(filter)
            .collect(Collectors.toList());
    }
}
