package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.Objects;

/**
 * Information about a cookie used in request and/or response.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#cookies">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarCookie {

    private String name;
    private String value;
    private String path;
    private String domain;
    private Date expires;
    private Boolean httpOnly;
    private Boolean secure;
    private String comment;

    /**
     * @return Name of the cookie, null if not present.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Value of the cookie, null if not present.
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return The cookie's path, null if not present.
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return The cookie's domain, null if not present.
     */
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return The cookie's expiration time, null if not present.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    /**
     * @return Whether the cookie is HTTP only, null if not present.
     */
    public Boolean getHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(Boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * @return Whether the cookie was transmitted via SSL, null if not present.
     */
    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
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
        HarCookie harCookie = (HarCookie) o;
        return Objects.equals(name, harCookie.name) &&
                Objects.equals(value, harCookie.value) &&
                Objects.equals(path, harCookie.path) &&
                Objects.equals(domain, harCookie.domain) &&
                Objects.equals(expires, harCookie.expires) &&
                Objects.equals(httpOnly, harCookie.httpOnly) &&
                Objects.equals(secure, harCookie.secure) &&
                Objects.equals(comment, harCookie.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, path, domain, expires, httpOnly, secure, comment);
    }
}
