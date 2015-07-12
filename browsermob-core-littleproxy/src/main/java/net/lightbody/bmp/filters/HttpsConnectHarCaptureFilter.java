package net.lightbody.bmp.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import net.lightbody.bmp.core.har.HarTimings;
import net.lightbody.bmp.filters.util.HarCaptureUtil;
import org.littleshoot.proxy.impl.ProxyUtils;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * This filter captures HAR data for HTTP CONNECT requests. CONNECTs are "meta" requests that must be made before HTTPS
 * requests, but are not populated as separate requests in the HAR. Most information from HTTP CONNECTs (such as SSL
 * handshake time, dns resolution time, etc.) is populated in the HAR entry for the first "true" request following the
 * CONNECT. This filter captures the timing-related information and makes it available to subsequent filters through
 * static methods. This filter also handles HTTP CONNECT errors and creates HAR entries for those errors, since there
 * would otherwise not be any record in the HAR of the error (if the CONNECT fails, there will be no subsequent "real"
 * request in which to record the error).
 *
 * TODO: refactor other HTTP CONNECT-specific logic out of HarCaptureFilter
 */
public class HttpsConnectHarCaptureFilter extends HttpsAwareFiltersAdapter {
    /**
     * The currently active HAR at the time the current request is received.
     */
    private final Har har;

    /**
     * The currently active page ref at the time the current request is received.
     */
    private final String currentPageRef;

    /**
     * The time this CONNECT began. Used to populate the HAR entry in case of failure.
     */
    private volatile Date requestStartTime;

    /**
     * Populated by proxyToServerResolutionStarted when DNS resolution starts. If any previous filters already resolved the address, their resolution time
     * will not be included in this time. See {@link HarCaptureFilter#dnsResolutionStartedNanos}.
     */
    private volatile long dnsResolutionStartedNanos;

    private volatile long dnsResolutionFinishedNanos;

    private volatile long connectionQueuedNanos;
    private volatile long connectionStartedNanos;
    private volatile long connectionSucceededTimeNanos;
    private volatile long sendStartedNanos;
    private volatile long sendFinishedNanos;

    private volatile long responseReceiveStartedNanos;
    private volatile long sslHandshakeStartedNanos;

    public HttpsConnectHarCaptureFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, Har har, String currentPageRef) {
        super(originalRequest, ctx);

        boolean httpConnect = ProxyUtils.isCONNECT(originalRequest);

        if (!httpConnect) {
            this.har = null;
            this.currentPageRef = null;
        } else {
            this.har = har;
            this.currentPageRef = currentPageRef;
        }
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (har == null) {
            return null;
        }

        if (httpObject instanceof HttpRequest) {
            // store the CONNECT start time in case of failure, so we can populate the HarEntry with it
            requestStartTime = new Date();
        }

        return null;
    }

    @Override
    public void proxyToServerResolutionFailed(String hostAndPort) {
        if (har == null) {
            return;
        }

        // since this is a CONNECT, which is not handled by the HarCaptureFilter, we need to create and populate the
        // entire HarEntry and add it to this har.
        HarEntry harEntry = createHarEntryForFailedCONNECT(HarCaptureUtil.getResolutionFailedErrorMessage(hostAndPort));
        har.getLog().addEntry(harEntry);

        // record the amount of time we attempted to resolve the hostname in the HarTimings object
        if (dnsResolutionStartedNanos > 0L) {
            harEntry.getTimings().setDns(System.nanoTime() - dnsResolutionStartedNanos, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void proxyToServerConnectionFailed() {
        if (har == null) {
            return;
        }

        // since this is a CONNECT, which is not handled by the HarCaptureFilter, we need to create and populate the
        // entire HarEntry and add it to this har.
        HarEntry harEntry = createHarEntryForFailedCONNECT(HarCaptureUtil.getConnectionFailedErrorMessage());
        har.getLog().addEntry(harEntry);

        // record the amount of time we attempted to connect in the HarTimings object
        if (connectionStartedNanos > 0L) {
            harEntry.getTimings().setConnect(System.nanoTime() - connectionStartedNanos, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void proxyToServerConnectionSucceeded() {
        if (har == null) {
            return;
        }

        this.connectionSucceededTimeNanos = System.nanoTime();
    }

    @Override
    public void proxyToServerConnectionSSLHandshakeStarted() {
        if (har == null) {
            return;
        }

        this.sslHandshakeStartedNanos = System.nanoTime();
    }

    @Override
    public void serverToProxyResponseTimedOut() {
        if (har == null) {
            return;
        }

        HarEntry harEntry = createHarEntryForFailedCONNECT(HarCaptureUtil.getResponseTimedOutErrorMessage());
        har.getLog().addEntry(harEntry);

        // include this timeout time in the HarTimings object
        long timeoutTimestampNanos = System.nanoTime();

        // if the proxy started to send the request but has not yet finished, we are currently "sending"
        if (sendStartedNanos > 0L && sendFinishedNanos == 0L) {
            harEntry.getTimings().setSend(timeoutTimestampNanos - sendStartedNanos, TimeUnit.NANOSECONDS);
        }
        // if the entire request was sent but the proxy has not begun receiving the response, we are currently "waiting"
        else if (sendFinishedNanos > 0L && responseReceiveStartedNanos == 0L) {
            harEntry.getTimings().setWait(timeoutTimestampNanos - sendFinishedNanos, TimeUnit.NANOSECONDS);
        }
        // if the proxy has already begun to receive the response, we are currenting "receiving"
        else if (responseReceiveStartedNanos > 0L) {
            harEntry.getTimings().setReceive(timeoutTimestampNanos - responseReceiveStartedNanos, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void proxyToServerConnectionQueued() {
        if (har == null) {
            return;
        }

        this.connectionQueuedNanos = System.nanoTime();
    }


    @Override
    public InetSocketAddress proxyToServerResolutionStarted(String resolvingServerHostAndPort) {
        if (har == null) {
            return null;
        }

        dnsResolutionStartedNanos = System.nanoTime();

        return null;
    }

    @Override
    public void proxyToServerResolutionSucceeded(String serverHostAndPort, InetSocketAddress resolvedRemoteAddress) {
        if (har == null) {
            return;
        }

        this.dnsResolutionFinishedNanos = System.nanoTime();
    }

    @Override
    public void proxyToServerConnectionStarted() {
        if (har == null) {
            return;
        }

        this.connectionStartedNanos = System.nanoTime();
    }

    @Override
    public void proxyToServerRequestSending() {
        if (har == null) {
            return;
        }

        this.sendStartedNanos = System.nanoTime();
    }

    @Override
    public void proxyToServerRequestSent() {
        if (har == null) {
            return;
        }

        this.sendFinishedNanos = System.nanoTime();
    }

    @Override
    public void serverToProxyResponseReceiving() {
        if (har == null) {
            return;
        }

        this.responseReceiveStartedNanos = System.nanoTime();
    }

    /**
     * Populates timing information in the specified harEntry for failed rquests. Populates as much timing information
     * as possible, up to the point of failure.
     *
     * @param harEntry HAR entry to populate timing information in
     */
    private void populateTimingsForFailedCONNECT(HarEntry harEntry) {
        HarTimings timings = harEntry.getTimings();

        if (connectionQueuedNanos > 0L && dnsResolutionStartedNanos > 0L) {
            timings.setBlocked(dnsResolutionStartedNanos - connectionQueuedNanos, TimeUnit.NANOSECONDS);
        }

        if (dnsResolutionStartedNanos > 0L && dnsResolutionFinishedNanos > 0L) {
            timings.setDns(dnsResolutionFinishedNanos - dnsResolutionStartedNanos, TimeUnit.NANOSECONDS);
        }

        if (connectionStartedNanos > 0L && connectionSucceededTimeNanos > 0L) {
            harEntry.getTimings().setConnect(connectionSucceededTimeNanos - connectionStartedNanos, TimeUnit.NANOSECONDS);

            if (sslHandshakeStartedNanos > 0L) {
                harEntry.getTimings().setSsl(connectionSucceededTimeNanos - this.sslHandshakeStartedNanos, TimeUnit.NANOSECONDS);
            }
        }

        if (sendStartedNanos > 0L && sendFinishedNanos >= 0L) {
            harEntry.getTimings().setSend(sendFinishedNanos - sendStartedNanos, TimeUnit.NANOSECONDS);
        }

        if (sendFinishedNanos > 0L && responseReceiveStartedNanos >= 0L) {
            harEntry.getTimings().setWait(responseReceiveStartedNanos - sendFinishedNanos, TimeUnit.NANOSECONDS);
        }

        // since this method is for HTTP CONNECT failures only, we can't populate a "received" time, since that would
        // require the CONNECT to be successful, in which case this method wouldn't be called.
    }

    /**
     * Creates a {@link HarEntry} for a failed CONNECT request. Initializes and populates the entry, including the
     * {@link HarRequest}, {@link HarResponse}, and {@link HarTimings}. (Note: only successful timing information is
     * populated in the timings object; the calling method must populate the timing information for the final, failed
     * step. For example, if DNS resolution failed, this method will populate the network 'blocked' time, but not the DNS
     * time.) Populates the specified errorMessage in the {@link HarResponse}'s error field.
     *
     * @param errorMessage error message to place in the har response
     * @return a new HAR entry
     */
    private HarEntry createHarEntryForFailedCONNECT(String errorMessage) {
        HarEntry harEntry = new HarEntry(currentPageRef);
        harEntry.setStartedDateTime(requestStartTime);

        HarRequest request = createRequestForFailedConnect(originalRequest);
        harEntry.setRequest(request);

        HarResponse response = HarCaptureUtil.createHarResponseForFailure();
        harEntry.setResponse(response);

        response.setError(errorMessage);

        // populate all timing information for this failed request
        populateTimingsForFailedCONNECT(harEntry);

        return harEntry;
    }

    /**
     * Creates a new {@link HarRequest} object for this failed HTTP CONNECT. Does not populate fields within the request,
     * such as the error message.
     *
     * @param httpConnectRequest the HTTP CONNECT request that failed
     * @return a new HAR request object
     */
    private HarRequest createRequestForFailedConnect(HttpRequest httpConnectRequest) {
        String url = getFullUrl(httpConnectRequest);

        return new HarRequest(httpConnectRequest.getMethod().toString(), url, httpConnectRequest.getProtocolVersion().text());
    }

}
