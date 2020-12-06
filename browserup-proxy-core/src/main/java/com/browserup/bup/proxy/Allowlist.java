/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A URL allowlist. This object is immutable and the list of matching patterns and the HTTP status code is unmodifiable
 * after creation. Enabling, disabling, or modifying the allowlist can be safely and easily accomplished by updating the
 * allowlist reference to a new allowlist.
 */
public class Allowlist {
    private final List<Pattern> patterns;
    private final int statusCode;
    private final boolean enabled;

    /**
     * A disabled Allowlist.
     */
    public static final Allowlist ALLOWLIST_DISABLED = new Allowlist();

    /**
     * Creates an empty, disabled Allowlist.
     */
    public Allowlist() {
        this.patterns = Collections.emptyList();
        this.statusCode = -1;
        this.enabled = false;
    }

    /**
     * Creates an empty, enabled allowlist with the specified response code.
     *
     * @param statusCode the response code that the (enabled) Allowlist will return for all URLs.
     */
    public Allowlist(int statusCode) {
        this.patterns = Collections.emptyList();
        this.statusCode = statusCode;
        this.enabled = true;
    }

    /**
     * @deprecated use {@link #Allowlist(java.util.Collection, int)}
     * @param patterns String[] patterns
     * @param statusCode int statusCode
     */
    @Deprecated
    public Allowlist(String[] patterns, int statusCode) {
        this(patterns == null ? null : Arrays.asList(patterns), statusCode);
    }

    /**
     * Creates a allowlist for the specified patterns, returning the given statusCode when a URL does not match one of the patterns.
     * A null or empty collection will result in an empty allowlist.
     *
     * @param patterns URL-matching regular expression patterns to allowlist
     * @param statusCode the HTTP status code to return when a request URL matches a allowlist pattern
     */
    public Allowlist(Collection<String> patterns, int statusCode) {
        if (patterns == null || patterns.isEmpty()) {
            this.patterns = Collections.emptyList();
        } else {
            ImmutableList.Builder<Pattern> builder = ImmutableList.builder();
            patterns.stream().map(Pattern::compile).forEach(builder::add);

            this.patterns = builder.build();
        }

        this.statusCode = statusCode;

        this.enabled = true;
    }

    /**
     * @return true if this allowlist is enabled, otherwise false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return regular expression patterns describing the URLs that should be allowlisted, or an empty collection if the allowlist is disabled
     */
    public Collection<Pattern> getPatterns() {
        return this.patterns;
    }

    /**
     * @return HTTP status code returned by the allowlist, or -1 if the allowlist is disabled
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @deprecated use {@link #getStatusCode()}
     * @return response code
     */
    @Deprecated
    public int getResponseCode() {
        return getStatusCode();
    }

    /**
     * Returns true if the specified URL matches a allowlisted URL regular expression. If the allowlist is disabled, this
     * method always returns false.
     *
     * @param url URL to match against the allowlist
     * @return true if the allowlist is enabled and the URL matched an entry in the allowlist, otherwise false
     */
    public boolean matches(String url) {
        if (!enabled) {
            return false;
        }

        return getPatterns().stream().map(pattern -> pattern.matcher(url)).anyMatch(Matcher::matches);
    }
}
