package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarResponse {

    protected static final Long DEFAULT_SIZE = -1L;

    private HttpStatus status;
    private String statusText;
    private String httpVersion;
    private List<HarCookie> cookies;
    private List<HarHeader> headers;
    private HarContent content;
    private String redirectURL;
    private Long headersSize;
    private Long bodySize;
    private String comment;
    private Map<String, Object> additional = new HashMap<>();

    /**
     * @return Response status, null if not present.
     */
    public int getStatus() {
        if (status == null) {
            status = HttpStatus.UNKNOWN_HTTP_STATUS;
        }
        return status.getCode();
    }

    public void setStatus(int status) {
        this.status = HttpStatus.byCode(status);
    }

    /**
     * @return Response status description, null if not present.
     */
    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    /**
     * @return Response HTTP Version, null if not present.
     */
    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * @return List of cookie objects.
     */
    public List<HarCookie> getCookies() {
        if (cookies == null) {
            cookies = new ArrayList<>();
        }
        return cookies;
    }

    public void setCookies(List<HarCookie> cookies) {
        this.cookies = cookies;
    }

    /**
     * @return List of header objects.
     */
    public List<HarHeader> getHeaders() {
        if (headers == null) {
            headers = new ArrayList<>();
        }
        return headers;
    }

    public void setHeaders(List<HarHeader> headers) {
        this.headers = headers;
    }

    /**
     * @return Details about the response body.
     */
    public HarContent getContent() {
        if (content == null) {
            content = new HarContent();
        }
        return content;
    }

    public void setContent(HarContent content) {
        this.content = content;
    }

    /**
     * @return Redirection target URL from the Location response header, null if not present.
     */
    public String getRedirectURL() {
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    /**
     * @return Total number of bytes from the start of the HTTP response message until (and including) the double
     * CRLF before the body. {@link #DEFAULT_SIZE} if the info is not available.
     */
    public Long getHeadersSize() {
        if (headersSize == null) {
            return DEFAULT_SIZE;
        }
        return headersSize;
    }

    public void setHeadersSize(Long headersSize) {
        this.headersSize = headersSize;
    }

    /**
     * @return Size of the received response body in bytes.
     * Set to zero in case of responses coming from the cache (304).
     * {@link #DEFAULT_SIZE} if the info is not available.
     */
    public Long getBodySize() {
        if (bodySize == null) {
            return DEFAULT_SIZE;
        }
        return bodySize;
    }

    public void setBodySize(Long bodySize) {
        this.bodySize = bodySize;
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

    @JsonAnyGetter
    public Map<String, Object> getAdditional() {
        return additional;
    }

    @JsonAnySetter
    public void setAdditionalField(String name, Object value) {
        this.additional.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HarResponse that = (HarResponse) o;
        return status == that.status &&
                Objects.equals(statusText, that.statusText) &&
                Objects.equals(httpVersion, that.httpVersion) &&
                Objects.equals(cookies, that.cookies) &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(content, that.content) &&
                Objects.equals(redirectURL, that.redirectURL) &&
                Objects.equals(headersSize, that.headersSize) &&
                Objects.equals(bodySize, that.bodySize) &&
                Objects.equals(comment, that.comment) &&
                Objects.equals(additional, that.additional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, statusText, httpVersion, cookies, headers, content, redirectURL, headersSize,
                bodySize, comment, additional);
    }
}
