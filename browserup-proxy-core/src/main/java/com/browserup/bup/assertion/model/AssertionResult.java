/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.assertion.model;

import com.browserup.bup.assertion.model.filter.AssertionFilterInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssertionResult {
    private String message;
    private Boolean failed;
    private Boolean passed;
    private Boolean errored;
    private AssertionFilterInfo filter;
    private List<AssertionEntryResult> requests;

    public AssertionResult() {
    }

    private AssertionResult(String message, Boolean failed, Boolean passed, Boolean errored,
                            AssertionFilterInfo filter, List<AssertionEntryResult> requests) {
        this.message = message;
        this.failed = failed;
        this.passed = passed;
        this.errored = errored;
        this.filter = filter;
        this.requests = requests;
    }

    public String getMessage() {
        return this.message;
    }

    public Boolean getFailed() {
        return this.failed;
    }

    public Boolean getPassed() {
        return this.passed;
    }

    public Boolean getErrored() {
        return this.errored;
    }

    public AssertionFilterInfo getFilter() {
        return this.filter;
    }

    public List<AssertionEntryResult> getRequests() {
        return this.requests;
    }

    public List<AssertionEntryResult> getFailedRequests() {
        return getRequests().stream()
                .filter(AssertionEntryResult::getFailed)
                .collect(Collectors.toList());
    }

    public static class Builder {
        private String message;
        private Boolean failed;
        private Boolean passed;
        private Boolean errored;
        private AssertionFilterInfo filter;
        private List<AssertionEntryResult> requests;

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setFailed(Boolean failed) {
            this.failed = failed;
            return this;
        }

        public Builder setPassed(Boolean passed) {
            this.passed = passed;
            return this;
        }

        public Builder setErrored(Boolean errored) {
            this.errored = errored;
            return this;
        }

        public Builder setFilter(AssertionFilterInfo filter) {
            this.filter = filter;
            return this;
        }

        public Builder setRequests(List<AssertionEntryResult> requests) {
            this.requests = requests;
            return this;
        }

        public Builder addRequest(AssertionEntryResult request) {
            if (this.requests == null) {
                this.requests = new ArrayList<>();
            }
            this.requests.add(request);
            return this;
        }

        public AssertionResult create() {
            if (StringUtils.isEmpty(message) || failed == null || filter == null || passed == null) {
                throw new IllegalArgumentException("Not all required fields are set");
            }
            requests = requests == null ? Collections.emptyList() : requests;
            return new AssertionResult(message, failed, passed, errored, filter, requests);
        }
    }
}