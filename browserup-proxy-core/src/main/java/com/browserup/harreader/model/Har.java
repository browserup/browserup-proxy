package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Objects;

/**
 * Main HTTP Archive Class.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/">speicification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Har {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    public void writeTo(Writer writer) throws IOException {
        OBJECT_MAPPER.writeValue(writer, this);
    }

    public void writeTo(OutputStream os) throws IOException {
        OBJECT_MAPPER.writeValue(os, this);
    }

    public void writeTo(File file) throws IOException {
        OBJECT_MAPPER.writeValue(file, this);
    }
}
