package com.browserup.bup.rest.openapi;

public class DocConstants {
    public static final String STATUS_DESCRIPTION = "Http status.";

    public static final String HEADER_NAME_PATTERN_DESCRIPTION = "Regular expression match of header name to find.";

    public static final String HEADER_VALUE_PATTERN_DESCRIPTION = "Regular expression match of header value.";

    public static final String HEADER_NAME_DESCRIPTION = "Header name";

    public static final String HEADER_VALUE_DESCRIPTION = "Header value";

    public static final String CONTENT_LENGTH_DESCRIPTION = "Max length of content, inclusive.";

    public static final String CONTENT_PATTERN_DESCRIPTION = "Regular expression match of content to find.";

    public static final String CONTENT_TEXT_DESCRIPTION = "String to search in the content.";

    public static final String PORT_DESCRIPTION = "Proxy port, use /proxy endpoint to get available proxy ports.";

    public static final String MILLISECONDS_DESCRIPTION = "Maximum time in milliseconds, inclusive.";

    public static final String URL_PATTERN_DESCRIPTION = "Regular expression match of URL to find.\n" +
            "URLs are formatted as: scheme://host:port/path?querystring.\n" +
            "Port is not included in the URL if it is the standard port for the scheme.\n" +
            "Fragments (example.com/#fragment) should not be included in the URL.\n" +
            "If more than one URL found, use the most recently requested URL.\n" +
            "Pattern examples:\n" +
            "- Match a URL with \"http\" or \"https\" protocol, \"example.com\" domain, and \"/index.html\" exact file path, with no query parameters:\n" +
            "  \"^(http|https)://example\\\\.com/index\\\\.html$\"\n" +
            "- Match a URL with \"http\" protocol, \"example.com\" domain, \"/customer\" exact path, followed by any query string:\n" +
            "  \"^http://example\\\\.com/customer\\\\?.*\"\n" +
            "- Match a URL with \"http\" protocol, \"example.com\" domain, \"/products\" path, and exactly 1 UUID query parameter named \"id\":\n" +
            "  \"^http://example\\\\.com/products\\\\?id=[0-9a-fA-F]{8}\\\\-[0-9a-fA-F]{4}\\\\-[0-9a-fA-F]{4}\\\\-[0-9a-fA-F]{4}\\\\-[0-9a-fA-F]{12}$\"\n";
}
