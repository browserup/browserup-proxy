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
 * Information about a single HTTP request.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#entries">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarEntry {

    private String pageref;
    private Date startedDateTime;
    private Integer time;
    private HarRequest request;
    private HarResponse response;
    private HarCache cache;
    private HarTiming timings;
    private String serverIPAddress;
    private String connection;
    private String comment;
    private Map<String, Object> additional = new HashMap<>();

    /**
     * @return Reference to parent page, to which the request belongs to, null if not present.
     */
    public String getPageref() {
        return pageref;
    }

    public void setPageref(String pageref) {
        this.pageref = pageref;
    }

    /**
     * @return Start time of request, null if not present.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Date getStartedDateTime() {
        return startedDateTime;
    }

    public void setStartedDateTime(Date startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    /**
     * @return Total request time (in ms), null if not present.
     */
    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    /**
     * @return Detailed request information.
     */
    public HarRequest getRequest() {
        if (request == null) {
            request = new HarRequest();
        }
        return request;
    }

    public void setRequest(HarRequest request) {
        this.request = request;
    }

    /**
     * @return Detailed response information.
     */
    public HarResponse getResponse() {
        if (response == null) {
            response = new HarResponse();
        }
        return response;
    }

    public void setResponse(HarResponse response) {
        this.response = response;
    }

    /**
     * @return Information about cache usage.
     */
    public HarCache getCache() {
        if (cache == null) {
            cache = new HarCache();
        }
        return cache;
    }

    public void setCache(HarCache cache) {
        this.cache = cache;
    }

    /**
     * @return Detailed information about request/response timings.
     */
    public HarTiming getTimings() {
        if (timings == null) {
            timings = new HarTiming();
        }
        return timings;
    }

    public void setTimings(HarTiming timings) {
        this.timings = timings;
    }

    /**
     * @return Server IP address (result of DNS resolution), null if not present.
     */
    public String getServerIPAddress() {
        return serverIPAddress;
    }

    public void setServerIPAddress(String serverIPAddress) {
        this.serverIPAddress = serverIPAddress;
    }

    /**
     * @return Unique ID of TCP/IP connection, null if not present.
     */
    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
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
        HarEntry harEntry = (HarEntry) o;
        return Objects.equals(pageref, harEntry.pageref) &&
                Objects.equals(startedDateTime, harEntry.startedDateTime) &&
                Objects.equals(time, harEntry.time) &&
                Objects.equals(request, harEntry.request) &&
                Objects.equals(response, harEntry.response) &&
                Objects.equals(cache, harEntry.cache) &&
                Objects.equals(timings, harEntry.timings) &&
                Objects.equals(serverIPAddress, harEntry.serverIPAddress) &&
                Objects.equals(connection, harEntry.connection) &&
                Objects.equals(comment, harEntry.comment) &&
                Objects.equals(additional, harEntry.additional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageref, startedDateTime, time, request, response, cache, timings, serverIPAddress,
                connection, comment, additional);
    }
}
