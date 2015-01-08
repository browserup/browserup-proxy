package net.lightbody.bmp.core.har;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HarResponse {
    private volatile int status;
    private volatile String statusText;
    private volatile String httpVersion;
    private final List<HarCookie> cookies = new CopyOnWriteArrayList<HarCookie>();
    private final List<HarNameValuePair> headers = new CopyOnWriteArrayList<HarNameValuePair>();
    private final HarContent content = new HarContent();
    private volatile String redirectURL = "";
    private volatile long headersSize;
    private volatile long bodySize;
    private volatile String comment = "";

    public HarResponse() {
    }

    public HarResponse(int status, String statusText, String httpVersion) {
        this.status = status;
        this.statusText = statusText;
        this.httpVersion = httpVersion;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public List<HarCookie> getCookies() {
        return cookies;
    }

    public List<HarNameValuePair> getHeaders() {
        return headers;
    }

    public HarContent getContent() {
        return content;
    }

    public String getRedirectURL() {
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    public long getHeadersSize() {
        return headersSize;
    }

    public void setHeadersSize(long headersSize) {
        this.headersSize = headersSize;
    }

    public long getBodySize() {
        return bodySize;
    }

    public void setBodySize(long bodySize) {
        this.bodySize = bodySize;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
