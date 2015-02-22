package net.lightbody.bmp.proxy.http;

import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarTimings;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestInfo {
    private static final Logger LOG = LoggerFactory.getLogger(RequestInfo.class);

    private static ThreadLocal<RequestInfo> instance = new ThreadLocal<RequestInfo>() {
        @Override
        protected RequestInfo initialValue() {
            return new RequestInfo();
        }
    };

    public static RequestInfo get() {
        return instance.get();
    }

    public static void clear(String url, HarEntry entry) {
        clear();
        RequestInfo info = get();
        info.url = url;
        info.entry = entry;
    }

    private static void clear() {
        RequestInfo info = get();
        info.blockedNanos = -1;
        info.dnsNanos = -1;
        info.connectNanos = -1;
        info.sslNanos = -1;
        info.sendNanos = 0;
        info.waitNanos = 0;
        info.receiveNanos = 0;
        info.resolvedAddress = null;
        info.startDate = null;
        info.startNanos = 0;
        info.endNanos = 0;
    }

    private long blockedNanos;
    private long dnsNanos;
    private long latencyNanos;
    private long connectNanos;
    // ssl timing can be populated from the separate ssl handshake notifier thread
    private volatile long sslNanos;
    private long sendNanos;
    private long waitNanos;
    private long receiveNanos;
    private String resolvedAddress;
    private Date startDate;
    private long startNanos;
    private long endNanos;
    private String url;
    private HarEntry entry;

    private long ping(long start, long end) {
        if (this.startDate == null || this.startNanos == 0) {
            LOG.error("Request start time was not set correctly; using current time");

            if (this.startDate == null) {
                this.startDate = new Date();
            }

            if (this.startNanos == 0) {
                this.startNanos = System.nanoTime();
            }
        }

        return end - start;
    }

    public Long getBlocked() {
        // return blocked;
        // purposely not sending back blocked timings for now until we know it's reliable
        return null;
    }

    public long getDns() {
        return dnsNanos;
    }

    public long getConnect() {
        return connectNanos;
    }

    public long getSsl() {
        return sslNanos;
    }

    public long getSend() {
        return sendNanos;
    }

    public long getWait() {
        return waitNanos;
    }

    public long getReceive() {
        return receiveNanos;
    }

    public String getResolvedAddress() {
        return resolvedAddress;
    }

    public void blocked(long start, long end) {
        // blocked is special - we don't record this start time as we don't want it to count towards receive time and
        // total time
        blockedNanos = end - start;
    }

    public void dns(long start, long end, String resolvedAddress) {
        dnsNanos = ping(start, end);
        this.resolvedAddress = resolvedAddress;
    }

    public void connect(long start, long end) {
        connectNanos = ping(start, end);
    }
    
    public void latency(long start, long end) {
    	latencyNanos = ping(start, end);
	}

    public void ssl(long start, long end) {
        sslNanos = ping(start, end);
    }

    public void send(long start, long end) {
        sendNanos = ping(start, end);
    }

    public void wait(long start, long end) {
        waitNanos = ping(start, end);
    }

    public void start() {
        this.startNanos = System.nanoTime();
        this.startDate = new Date();
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public void finish() {
        if (startDate == null) {
            startDate = new Date();
        }

        if (startNanos == 0) {
            startNanos = System.nanoTime();
        }

        endNanos = System.nanoTime();

        receiveNanos = endNanos - startNanos - norm(waitNanos) - norm(sendNanos) - norm(sslNanos) - norm(connectNanos) - norm(dnsNanos);

        // as per the Har 1.2 spec (to maintain backwards compatibility with 1.1) the connect time should actually
        // include the ssl handshaking time, so doing that here after everything has been calculated
        if (norm(sslNanos) > 0L) {
            connectNanos += sslNanos;
        }

        if (receiveNanos < 0L) {
            LOG.error("Got a negative receiving time ({}) for URL {}", receiveNanos, url);
            receiveNanos = 0L;
        }
    }

    private long norm(Long val) {
        if (val == null || val == -1) {
            return 0;
        } else {
            return val;
        }
    }

    public long getTotalTime(TimeUnit timeUnit) {
        if (endNanos == 0 || startNanos == 0) {
            return -1;
        }

        return timeUnit.convert(endNanos - startNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public String toString() {
        long totalTimeNanos = getTotalTime(TimeUnit.NANOSECONDS);

        return "RequestInfo{" +
                "blocked=" + blockedNanos + "ns" +
                ", dns=" + dnsNanos + "ns" +
                ", connect=" + connectNanos + "ns" +
                ", ssl=" + sslNanos + "ns" +
                ", send=" + sendNanos + "ns" +
                ", wait=" + waitNanos + "ns" +
                ", receive=" + receiveNanos + "ns" +
                ", total=" + totalTimeNanos + "ns" +
                ", resolvedAddress='" + resolvedAddress + '\'' +
                '}';
    }

    public HarTimings getTimings() {
        HarTimings harTimings = new HarTimings();
        harTimings.setBlocked(blockedNanos, TimeUnit.NANOSECONDS);
        harTimings.setDns(dnsNanos, TimeUnit.NANOSECONDS);
        harTimings.setConnect(connectNanos, TimeUnit.NANOSECONDS);
        harTimings.setSend(sendNanos, TimeUnit.NANOSECONDS);
        harTimings.setWait(waitNanos, TimeUnit.NANOSECONDS);
        harTimings.setReceive(receiveNanos, TimeUnit.NANOSECONDS);
        harTimings.setSsl(sslNanos, TimeUnit.NANOSECONDS);

        return harTimings;
    }

    public HarEntry getEntry() {
        return entry;
    }

	public long getLatency(TimeUnit timeUnit) {
		return timeUnit.convert(latencyNanos, TimeUnit.NANOSECONDS);
	}
}
