package com.browserup.bup.assertion.model;

import com.browserup.bup.assertion.model.filter.AssertionFilterInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssertionResult {
    private String result;
    private Boolean failed;
    private Boolean errored;
    private AssertionFilterInfo filter;
    private List<AssertionEntryResult> requests;

    private AssertionResult(String result, Boolean failed, Boolean errored,
                            AssertionFilterInfo filter, List<AssertionEntryResult> requests) {
        this.result = result;
        this.failed = failed;
        this.errored = errored;
        this.filter = filter;
        this.requests = requests;
    }

    public String getResult() {
        return result;
    }

    public Boolean getFailed() {
        return failed;
    }

    public Boolean getErrored() {
        return errored;
    }

    public AssertionFilterInfo getFilter() {
        return filter;
    }

    public List<AssertionEntryResult> getRequests() {
        return requests;
    }

    public static class Builder {
        private String result;
        private Boolean failed;
        private Boolean errored;
        private AssertionFilterInfo filter;
        private List<AssertionEntryResult> requests;

        public Builder setResult(String result) {
            this.result = result;
            return this;
        }

        public Builder setFailed(Boolean failed) {
            this.failed = failed;
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
            if (StringUtils.isEmpty(result) || failed == null || filter == null) {
                throw new IllegalArgumentException("Not all required fields are set");
            }
            return new AssertionResult(result, failed, errored, filter, requests);
        }
    }
}