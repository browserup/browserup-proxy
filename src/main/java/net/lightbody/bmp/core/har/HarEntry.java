package net.lightbody.bmp.core.har;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.lightbody.bmp.core.json.ISO8601WithTDZDateFormatter;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect
public class HarEntry {
    private volatile String pageref;
    private volatile Date startedDateTime;
    private volatile HarRequest request;
    private volatile HarResponse response;
    private volatile HarCache cache = new HarCache();
    private volatile HarTimings timings = new HarTimings();
    private volatile String serverIPAddress;
    private volatile String connection;
    private volatile String comment = "";

    public HarEntry() {
    }

    public HarEntry(String pageref) {
        this.pageref = pageref;
    }

    public String getPageref() {
        return pageref;
    }

    public void setPageref(String pageref) {
        this.pageref = pageref;
    }

    @JsonSerialize(using = ISO8601WithTDZDateFormatter.class)
    public Date getStartedDateTime() {
        return startedDateTime;
    }

    public void setStartedDateTime(Date startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    /**
     * Rather than storing the time directly, calculate the time from the HarTimings as required in the HAR spec.
     * From <a href="https://dvcs.w3.org/hg/webperf/raw-file/tip/specs/HAR/Overview.html">https://dvcs.w3.org/hg/webperf/raw-file/tip/specs/HAR/Overview.html</a>,
     * section <code>4.2.16 timings</code>:
     <pre>
     Following must be true in case there are no -1 values (entry is an object in log.entries) :

     entry.time == entry.timings.blocked + entry.timings.dns +
     entry.timings.connect + entry.timings.send + entry.timings.wait +
     entry.timings.receive;
     </pre>
     * @return
     */
    public long getTime() {
        HarTimings timings = getTimings();
        if (timings != null) {
            int time = 0;
            if (timings.getBlocked() != null && timings.getBlocked() > 0) {
                time += timings.getBlocked();
            }

            if (timings.getDns() != null && timings.getDns() > 0) {
                time += timings.getDns();
            }

            if (timings.getConnect() != null && timings.getConnect() > 0) {
                time += timings.getConnect();
            }

            if (timings.getSend() > 0) {
                time += timings.getSend();
            }

            if (timings.getWait() > 0) {
                time += timings.getWait();
            }

            if (timings.getReceive() > 0) {
                time += timings.getReceive();
            }

            return time;
        }

        return -1;
    }

    public HarRequest getRequest() {
        return request;
    }

    public void setRequest(HarRequest request) {
        this.request = request;
    }

    public HarResponse getResponse() {
        return response;
    }

    public void setResponse(HarResponse response) {
        this.response = response;
    }

    public HarCache getCache() {
        return cache;
    }

    public void setCache(HarCache cache) {
        this.cache = cache;
    }

    public HarTimings getTimings() {
        return timings;
    }

    public void setTimings(HarTimings timings) {
        this.timings = timings;
    }

    public String getServerIPAddress() {
        return serverIPAddress;
    }

    public void setServerIPAddress(String serverIPAddress) {
        this.serverIPAddress = serverIPAddress;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }
}
