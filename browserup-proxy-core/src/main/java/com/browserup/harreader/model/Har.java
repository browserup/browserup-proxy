package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Main HTTP Archive Class.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/">speicification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Har {

    private HarLog log;

    /**
     * @return HAR log.
     */
    public HarLog getLog() {
        if (log == null) {
            log = new HarLog();
        }
        return log;
    }

    public void setLog(HarLog log) {
        this.log = log;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Har har = (Har) o;
        return Objects.equals(log, har.log);
    }

    @Override
    public int hashCode() {
        return Objects.hash(log);
    }
}
