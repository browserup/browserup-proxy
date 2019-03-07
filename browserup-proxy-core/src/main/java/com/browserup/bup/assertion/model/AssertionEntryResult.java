package com.browserup.bup.assertion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssertionEntryResult {
    private String url;
    private String message;
    private Boolean failed;

    private AssertionEntryResult(String url, String message, Boolean failed) {
        this.url = url;
        this.message = message;
        this.failed = failed;
    }

    public String getUrl() {
        return url;
    }

    public String getMessage() {
        return message;
    }

    public Boolean getFailed() {
        return failed;
    }

    public static class Builder {
        private String url;
        private String message;
        private Boolean failed;

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setFailed(Boolean failed) {
            this.failed = failed;
            return this;
        }

        public AssertionEntryResult create() {
            if (StringUtils.isAnyEmpty(url, message) || failed == null) {
                throw new IllegalArgumentException("Not all required fields are set");
            }
            return new AssertionEntryResult(url, message, failed);
        }
    }
}