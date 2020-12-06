/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy;

import java.util.regex.Pattern;

/**
 * An entry in the Blocklist, consisting of a regular expression to match the URL, an HTTP status code, and a regular expression
 * to match the HTTP method.
 */
public class BlocklistEntry {
    private final Pattern urlPattern;
    private final int statusCode;
    private final Pattern httpMethodPattern;

    /**
     * Creates a new BlocklistEntry with no HTTP method matching (i.e. all methods will match).
     *
     * @param urlPattern URL pattern to blocklist
     * @param statusCode HTTP status code to return for blocklisted URL
     */
    public BlocklistEntry(String urlPattern, int statusCode) {
        this(urlPattern, statusCode, null);
    }

    /**
     * Creates a new BlocklistEntry which will match both a URL and an HTTP method
     *
     * @param urlPattern URL pattern to blocklist
     * @param statusCode status code to return for blocklisted URL
     * @param httpMethodPattern HTTP method to match (e.g. GET, PUT, PATCH, etc.)
     */
    public BlocklistEntry(String urlPattern, int statusCode, String httpMethodPattern) {
        this.urlPattern = Pattern.compile(urlPattern);
        this.statusCode = statusCode;
        if (httpMethodPattern == null || httpMethodPattern.isEmpty()) {
            this.httpMethodPattern = null;
        } else {
            this.httpMethodPattern = Pattern.compile(httpMethodPattern);
        }
    }

    /**
     * Determines if this BlocklistEntry matches the given URL. Attempts to match both the URL and the
     * HTTP method.
     *
     * @param url possibly-blocklisted URL
     * @param httpMethod HTTP method this URL is being accessed with
     * @return true if the URL matches this BlocklistEntry
     */
    public boolean matches(String url, String httpMethod) {
        if (httpMethodPattern != null) {
            return urlPattern.matcher(url).matches() && httpMethodPattern.matcher(httpMethod).matches();
        } else {
            return urlPattern.matcher(url).matches();
        }
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Pattern getHttpMethodPattern() {
        return httpMethodPattern;
    }

    @Deprecated
    /**
     * @deprecated use {@link #getUrlPattern()}
     */
    public Pattern getPattern() {
        return getUrlPattern();
    }

    @Deprecated
    /**
     * @deprecated use {@link #getStatusCode()}
     */
    public int getResponseCode() {
        return getStatusCode();
    }

    @Deprecated
    /**
     * @deprecated use {@link #getHttpMethodPattern()}
     */
    public Pattern getMethod() {
        return getHttpMethodPattern();
    }
}