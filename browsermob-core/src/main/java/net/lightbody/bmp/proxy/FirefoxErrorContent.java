package net.lightbody.bmp.proxy;

import com.google.common.html.HtmlEscapers;
import net.lightbody.bmp.l10n.MessagesUtil;

/**
 * Proxy errors that can be returned to the client. This enum is a convenience class that collects the title, short description, and
 * long description for each error type, and provides a {@link #getHtml(String)} method to return formatted HTML code to return to the
 * client.
 */
public enum FirefoxErrorContent {
    CONN_FAILURE(
            "response.conn_failure.title",
            "response.conn_failure.short",
            "response.common_error.long"),
    DNS_NOT_FOUND(
            "response.dns_not_found.title",
            "response.dns_not_found.short",
            "response.dns_not_found.long"),
    GENERIC(
            "response.generic.title",
            "response.generic.short",
            "response.generic.long"),
    MALFORMED_URI(
            "response.malformed_uri.title",
            "response.malformed_uri.short",
            "response.malformed_uri.long"),
    NET_INTERRUPT(
            "response.net_interrupt.title",
            "response.net_interrupt.short",
            "response.common_error.long"),
    NET_RESET(
            "response.net_reset.title",
            "response.net_reset.short",
            "response.common_error.long"),
    NET_TIMEOUT(
            "response.net_timeout.title",
            "response.net_timeout.short",
            "response.common_error.long"),
    ;

    private final String title;
    private final String shortDesc;
    private final String longDesc;

    FirefoxErrorContent(String titleMessageKey, String shortDescMessageKey, String longDescMessageKey) {
        this.title = MessagesUtil.getMessage(titleMessageKey);
        this.shortDesc = MessagesUtil.getMessage(shortDescMessageKey);
        this.longDesc = MessagesUtil.getMessage(longDescMessageKey);
    }

    /**
     * Returns an HTML message for this error that can be sent to the client.
     *
     * @param url URL request that caused the error
     * @return HTML for this error
     */
    public String getHtml(String url) {
        String formattedShortDesc = String.format(shortDesc, HtmlEscapers.htmlEscaper().escape(url));

        return MessagesUtil.getMessage("response.error_page.html", title, formattedShortDesc, longDesc);
    }

}
