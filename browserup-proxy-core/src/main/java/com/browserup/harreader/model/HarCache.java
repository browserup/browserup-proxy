package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.Objects;

/**
 * Information about a request coming from browser cache.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#cache">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarCache {

    private HarCacheInfo beforeRequest;
    private HarCacheInfo afterRequest;
    private String comment;

    /**
     * @return State of the cache entry before the request, null if not present.
     */
    public HarCacheInfo getBeforeRequest() {
        return beforeRequest;
    }

    public void setBeforeRequest(HarCacheInfo beforeRequest) {
        this.beforeRequest = beforeRequest;
    }

    /**
     * @return State of the cache entry after the request, null if not present.
     */
    public HarCacheInfo getAfterRequest() {
        return afterRequest;
    }

    public void setAfterRequest(HarCacheInfo afterRequest) {
        this.afterRequest = afterRequest;
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
        HarCache harCache = (HarCache) o;
        return Objects.equals(beforeRequest, harCache.beforeRequest) &&
                Objects.equals(afterRequest, harCache.afterRequest) &&
                Objects.equals(comment, harCache.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beforeRequest, afterRequest, comment);
    }

    /**
     * Information about a request coming from browser cache.
     * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#cache">specification</a>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class HarCacheInfo {

        private Date expires;
        private Date lastAccess;
        private String eTag;
        private Integer hitCount;
        private String comment;

        /**
         * @return Expiration time of entry, null if not present.
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public Date getExpires() {
            return expires;
        }

        public void setExpires(Date expires) {
            this.expires = expires;
        }

        /**
         * @return Last time the entry was opened, null if not present.
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public Date getLastAccess() {
            return lastAccess;
        }

        public void setLastAccess(Date lastAccess) {
            this.lastAccess = lastAccess;
        }

        /**
         * @return ETag, null if not present.
         */
        public String geteTag() {
            return eTag;
        }

        public void seteTag(String eTag) {
            this.eTag = eTag;
        }

        /**
         * @return Number of times the entry has been opened, null if not present.
         */
        public Integer getHitCount() {
            return hitCount;
        }

        public void setHitCount(Integer hitCount) {
            this.hitCount = hitCount;
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
            HarCacheInfo that = (HarCacheInfo) o;
            return Objects.equals(expires, that.expires) &&
                    Objects.equals(lastAccess, that.lastAccess) &&
                    Objects.equals(eTag, that.eTag) &&
                    Objects.equals(hitCount, that.hitCount) &&
                    Objects.equals(comment, that.comment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expires, lastAccess, eTag, hitCount, comment);
        }
    }
}
