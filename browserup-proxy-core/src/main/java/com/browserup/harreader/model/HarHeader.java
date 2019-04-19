package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Information about a header used in request and/or response.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#headers">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarHeader {

    private String name;
    private String value;
    private String comment;

    /**
     * @return Header name, null if not present.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Header value, null if not present.
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
        HarHeader harHeader = (HarHeader) o;
        return Objects.equals(name, harHeader.name) &&
                Objects.equals(value, harHeader.value) &&
                Objects.equals(comment, harHeader.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, comment);
    }
}
