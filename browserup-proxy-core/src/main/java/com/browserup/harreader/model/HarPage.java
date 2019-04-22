package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Information about an exported page.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#pages">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarPage {

    private Date startedDateTime;
    private String id;
    private String title;
    private HarPageTiming pageTimings;
    private String comment;
    private Map<String, Object> additional = new HashMap<>();

    /**
     * @return Start time of page load, null if not present.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Date getStartedDateTime() {
        return startedDateTime;
    }

    public void setStartedDateTime(Date startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    /**
     * @return Unique identifier, null if not present.
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Page title, null if not present.
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return Detailed information about page loading timings.
     */
    public HarPageTiming getPageTimings() {
        if (pageTimings == null) {
            pageTimings = new HarPageTiming();
        }
        return pageTimings;
    }

    public void setPageTimings(HarPageTiming pageTimings) {
        this.pageTimings = pageTimings;
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
        HarPage harPage = (HarPage) o;
        return Objects.equals(startedDateTime, harPage.startedDateTime) &&
                Objects.equals(id, harPage.id) &&
                Objects.equals(title, harPage.title) &&
                Objects.equals(pageTimings, harPage.pageTimings) &&
                Objects.equals(comment, harPage.comment) &&
                Objects.equals(additional, harPage.additional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startedDateTime, id, title, pageTimings, comment, additional);
    }
}
