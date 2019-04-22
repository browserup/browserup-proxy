package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * This class is a merge of these 2 HarTiming implementations:
 * * https://github.com/lightbody/browsermob-proxy/blob/master/browsermob-core/src/main/java/net/lightbody/bmp/core/har/HarTimings.java
 * * https://github.com/sdstoehr/har-reader/blob/master/src/main/java/de/sstoehr/harreader/model/HarTiming.java
 *
 * It primarily differs from the de.sdstoehr implementation in that it internally stores
 * metrics with nanosecond precision.
 * In the JSON serialized form, the nanoseconds are converted to milliseconds,
 * in accordance with the HAR spec. Some precision is lost in the TimeUnit conversion.
 * Specifically, TimeUnit will truncate the Long nanosecond value to fit into an Integer,
 * without rounding. For example, 999,999 ns == 0 ms, and 1,000,000 ns = 1 ms.
 * Storing the times internally as nanoseconds makes it easier to write unit tests,
 * such as when checking that fast-running operations took &gt; 0ms to complete.
 *
 * The de.sdstoehr implementation differs from the lightbody implementation in that
 * it serializes both ways, and is more up to date on the HAR specification, such as
 * it supports additional fields added to metrics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarTiming {

    // optional values are initialized to -1, which indicates they do not apply to the current request, according to the HAR spec
    private volatile long blockedNanos = -1;
    private volatile long dnsNanos = -1;
    private volatile long connectNanos = -1;
    private volatile long sslNanos = -1;
    // Per HAR spec, the send, wait and receive timings are not optional and must have non-negative values.
    private volatile long sendNanos = 0;
    private volatile long waitNanos = 0;
    private volatile long receiveNanos = 0;
    private volatile String comment = "";

    /**
     * @return Time spent in a queue waiting for a network connection.
     * -1 if the timing does not apply to the current request.
     * @param timeUnit param
     */
    public long getBlocked(TimeUnit timeUnit) {
        if (blockedNanos == -1) {
            return -1;
        } else {
            return timeUnit.convert(blockedNanos, TimeUnit.NANOSECONDS);
        }
    }

    public void setBlocked(long blocked, TimeUnit timeUnit) {
        if (blocked == -1) {
            this.blockedNanos = -1;
        } else {
            this.blockedNanos = TimeUnit.NANOSECONDS.convert(blocked, timeUnit);
        }
    }

    /**
     * @return DNS resolution time. The time required to resolve a host name.
     * -1 if the timing does not apply to the current request.
     * @param timeUnit param
     */
    public long getDns(TimeUnit timeUnit) {
        if (dnsNanos == -1) {
            return -1;
        } else {
            return timeUnit.convert(dnsNanos, TimeUnit.NANOSECONDS);
        }
    }

    public void setDns(long dns, TimeUnit timeUnit) {
        if (dns == -1) {
            this.dnsNanos = -1;
        } else{
            this.dnsNanos = TimeUnit.NANOSECONDS.convert(dns, timeUnit);
        }
    }

    /**
     * @return Time required to create TCP connection.
     * -1 if the timing does not apply to the current request.
     * @param timeUnit param
     */
    public long getConnect(TimeUnit timeUnit) {
        if (connectNanos == -1) {
            return -1;
        } else {
            return timeUnit.convert(connectNanos, TimeUnit.NANOSECONDS);
        }
    }

    public void setConnect(long connect, TimeUnit timeUnit) {
        if (connect == -1) {
            this.connectNanos = -1;
        } else {
            this.connectNanos = TimeUnit.NANOSECONDS.convert(connect, timeUnit);
        }
    }

    /**
     * @return Time required to send HTTP request to the server, 0 if not present.
     * According to the HAR spec, the send, wait and receive timings are not optional and must have non-negative values.
     * @param timeUnit param
     */
    public long getSend(TimeUnit timeUnit) {
        return timeUnit.convert(sendNanos, TimeUnit.NANOSECONDS);
    }

    public void setSend(long send, TimeUnit timeUnit) {
        this.sendNanos = TimeUnit.NANOSECONDS.convert(send, timeUnit);
    }

    /**
     * @return Time spent waiting for a response from the server, 0 if not present.
     * According to the HAR spec, the send, wait and receive timings are not optional and must have non-negative values.
     * @param timeUnit param
     */
    public long getWait(TimeUnit timeUnit) {
        return timeUnit.convert(waitNanos, TimeUnit.NANOSECONDS);
    }

    public void setWait(long wait, TimeUnit timeUnit) {
        this.waitNanos = TimeUnit.NANOSECONDS.convert(wait, timeUnit);
    }

    /**
     * @return Time spent reading the entire response from the server, 0 if not present.
     * According to the HAR spec, the send, wait and receive timings are not optional and must have non-negative values.
     * @param timeUnit param
     */
    public long getReceive(TimeUnit timeUnit) {
        return timeUnit.convert(receiveNanos, TimeUnit.NANOSECONDS);
    }

    public void setReceive(long receive, TimeUnit timeUnit) {
        this.receiveNanos = TimeUnit.NANOSECONDS.convert(receive, timeUnit);
    }

    /**
     * @return Time required for SSL/TLS negotiation.
     * If this field is defined then the time is also included in the connect field
     * (to ensure backward compatibility with HAR 1.1).
     * -1 if the timing does not apply to the current request.
     * @param timeUnit param
     */
    public long getSsl(TimeUnit timeUnit) {
        if (sslNanos == -1) {
            return -1;
        } else {
            return timeUnit.convert(sslNanos, TimeUnit.NANOSECONDS);
        }
    }

    public void setSsl(long ssl, TimeUnit timeUnit) {
        if (ssl == -1) {
            this.sslNanos = -1;
        } else {
            this.sslNanos = TimeUnit.NANOSECONDS.convert(ssl, timeUnit);
        }
    }

    // -------------------------------------------------------------------

    // The following getters and setters assume TimeUnit.MILLISECOND precision. this allows jackson to generate ms values (in accordance
    // with the HAR spec), and also preserves compatibility with the legacy methods. optional methods are also declared as Long instead of
    // long (even though they always have values), to preserve compatibility. in general, the getters/setters which take TimeUnits
    // should always be preferred.

    /**
     * @return Time spent in a queue waiting for a network connection, in milliseconds.
     * -1 if the timing does not apply to the current request.
     */
    public int getBlocked() {
        return Math.toIntExact(getBlocked(TimeUnit.MILLISECONDS));
    }

    public void setBlocked(Integer blocked) {
        if (blocked == null) blocked = -1;
        setBlocked(blocked, TimeUnit.MILLISECONDS);
    }

    /**
     * @return DNS resolution time. The time required to resolve a host name.
     * -1 if the timing does not apply to the current request.
     */
    public int getDns() {
        return Math.toIntExact(getDns(TimeUnit.MILLISECONDS));
    }

    public void setDns(Integer dns) {
        if (dns == null) dns = -1;
        setDns(dns, TimeUnit.MILLISECONDS);
    }

    /**
     * @return Time required to create TCP connection.
     * -1 if the timing does not apply to the current request.
     */
    public int getConnect() {
        return Math.toIntExact(getConnect(TimeUnit.MILLISECONDS));
    }

    public void setConnect(Integer connect) {
        if (connect == null) connect = -1;
        setConnect(connect, TimeUnit.MILLISECONDS);
    }

    /**
     * @return Time required to send HTTP request to the server, in milliseconds.
     * Returns 0 if not present.
     * According to the HAR spec, the send, wait and receive timings are not optional and must have non-negative values.
     */
    public int getSend() {
        return Math.toIntExact(getSend(TimeUnit.MILLISECONDS));
    }

    public void setSend(Integer send) {
        if (send == null) send = 0;
        setSend(send, TimeUnit.MILLISECONDS);
    }

    /**
     * @return Time spent waiting for a response from the server, in milliseconds.
     * Returns 0 if not present.
     * According to the HAR spec, the send, wait and receive timings are not optional and must have non-negative values.
     */
    public int getWait() {
        return Math.toIntExact(getWait(TimeUnit.MILLISECONDS));
    }

    public void setWait(Integer wait) {
        if (wait == null) wait = 0;
        setWait(wait, TimeUnit.MILLISECONDS);
    }

    /**
     * @return Time spent reading the entire response from the server, in milliseconds.
     * Returns 0 if not present.
     * According to the HAR spec, the send, wait and receive timings are not optional and must have non-negative values.
     */
    public int getReceive() {
        return Math.toIntExact(getReceive(TimeUnit.MILLISECONDS));
    }

    public void setReceive(Integer receive) {
        if (receive == null) receive = 0;
        setReceive(receive, TimeUnit.MILLISECONDS);
    }

    /**
     * @return Time required for SSL/TLS negotiation.
     * If this field is defined then the time is also included in the connect field
     * (to ensure backward compatibility with HAR 1.1).
     * -1 if the timing does not apply to the current request.
     */
    public int getSsl() {
        return Math.toIntExact(getSsl(TimeUnit.MILLISECONDS));
    }

    public void setSsl(Integer ssl) {
        if (ssl == null) ssl = -1;
        setSsl(ssl, TimeUnit.MILLISECONDS);
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
        HarTiming harTiming = (HarTiming) o;
        return Objects.equals(blockedNanos, harTiming.blockedNanos) &&
                Objects.equals(dnsNanos, harTiming.dnsNanos) &&
                Objects.equals(connectNanos, harTiming.connectNanos) &&
                Objects.equals(sendNanos, harTiming.sendNanos) &&
                Objects.equals(waitNanos, harTiming.waitNanos) &&
                Objects.equals(receiveNanos, harTiming.receiveNanos) &&
                Objects.equals(sslNanos, harTiming.sslNanos) &&
                Objects.equals(comment, harTiming.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockedNanos, dnsNanos, connectNanos, sendNanos, waitNanos, receiveNanos, sslNanos, comment);
    }
}
