package net.lightbody.bmp.filters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarCookie;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarNameVersion;
import net.lightbody.bmp.core.har.HarPostData;
import net.lightbody.bmp.core.har.HarPostDataParam;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import net.lightbody.bmp.proxy.CaptureType;
import net.lightbody.bmp.proxy.util.BrowserMobProxyUtil;
import net.lightbody.bmp.util.BrowserMobHttpUtil;
import net.sf.uadetector.ReadableUserAgent;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HarCaptureFilter extends HttpFiltersAdapter {
    private static final Logger log = LoggerFactory.getLogger(HarCaptureFilter.class);

    private final Har har;

    /**
     * The harEntry is created when this filter is constructed and is shared by both the clientToProxyRequest
     * and serverToProxyResponse methods. It is added to the HarLog when the request is received from the client.
     */
    private final HarEntry harEntry;

    /**
     * The requestCaptureFilter captures all request content, including headers, trailing headers, and content. The HarCaptureFilter
     * delegates to it when the clientToProxyRequest() callback is invoked. If this request does not need content capture, the
     * ClientRequestCaptureFilter filter will not be instantiated and will not capture content.
     */
    private final ClientRequestCaptureFilter requestCaptureFilter;

    /**
     * Like requestCaptureFilter above, HarCaptureFilter delegates to responseCaptureFilter to capture response contents. If content capture
     * is not required for this request, the filter will not be instantiated or invoked.
     */
    private final ServerResponseCaptureFilter responseCaptureFilter;

    /**
     * The CaptureType data types to capture in this request.
     */
    private final EnumSet<CaptureType> dataToCapture;

    /**
     * Populated by proxyToServerResolutionStarted when DNS resolution starts. If any previous filters already resolved the address, their resolution time
     * will not be included in this time.
     */
    private volatile long dnsResolutionStartedNanos;

    private volatile long connectionQueuedNanos;
    private volatile long connectionStartedNanos;

    private volatile long sslHandshakeStartedNanos;

    private volatile long sendStartedNanos;
    private volatile long sendFinishedNanos;

    private volatile long responseReceiveStartedNanos;

    /**
     * True if this is an HTTP CONNECT request, for which some special timing information is needed.
     */
    private final boolean httpConnect;

    /**
     * The address of the client making the request. Captured in the constructor and used when calculating and capturing ssl handshake and connect
     * timing information for SSL connections.
     */
    private final InetSocketAddress clientAddress;

    /**
     * Request body size is determined by the actual size of the data the client sends. The filter does not use the Content-Length header to determine request size.
     */
    private final AtomicInteger requestBodySize = new AtomicInteger(0);

    /**
     * Response body size is determined by the actual size of the data the server sends.
     */
    private final AtomicInteger responseBodySize = new AtomicInteger(0);

    /**
     * The "real" original request, as captured by the {@link #clientToProxyRequest(io.netty.handler.codec.http.HttpObject)} method.
     */
    private volatile HttpRequest capturedOriginalRequest;

    /**
     * True if this filter instance processed a {@link #proxyToServerResolutionSucceeded(String, java.net.InetSocketAddress)} call, indicating
     * that the hostname was resolved and populated in the HAR (if this is not a CONNECT).
     */
    private volatile boolean addressResolved = false;

    /**
     * The maximum amount of time to save timing information between an HTTP CONNECT and the subsequent HTTP request. Typically this is done
     * immediately, but if for some reason it is not (e.g. due to a client crash or dropped connection), the timing information will be
     * kept for this long before being evicted to prevent a memory leak. If a subsequent request does come through after eviction, it will still
     * be recorded, but the timing information will not be populated in the HAR.
     */
    private static final int HTTP_CONNECT_TIMING_EVICTION_SECONDS = 60;

    /**
     * The maximum amount of time to save host name resolution information. This is done in order to populate the server IP address field in the
     * har. Unfortunately there is not currently any way to determine the remote IP address of a keep-alive connection in a filter, so caching the
     * resolved hostnames gives a generally-reasonable best guess.
     */
    private static final int RESOLVED_ADDRESSES_EVICTION_SECONDS = 300;

    /**
     * Concurrency of the httpConnectTiming map. Should be approximately equal to the maximum number of simultaneous connection
     * attempts (but not necessarily simultaneous connections). A lower value will inhibit performance.
     * TODO: tune this value for a large number of concurrent requests. develop a non-cache-based mechanism of passing ssl timings to subsequent requests.
     */
    private static final int HTTP_CONNECT_TIMING_CONCURRENCY_LEVEL = 50;

    /**
     * Stores HTTP CONNECT timing information for this request, if it is an HTTP CONNECT.
     */
    private final HttpConnectTiming httpConnectTiming;

    /**
     * Stores SSL connection timing information from HTTP CONNNECT requests. This timing information is stored in the first HTTP request
     * after the CONNECT, not in the CONNECT itself, so it needs to be stored across requests.
     *
     * This is the only state stored across multiple requests.
     */
    private static final ConcurrentMap<InetSocketAddress, HttpConnectTiming> httpConnectTimes;

    /**
     * A {@code Map<hostname, IP address>} that provides a reasonable estimate of the upstream server's IP address for keep-alive connections.
     * The expiration time is renewed after each access, rather than after each write, so if the connection is consistently kept alive and used,
     * the cached IP address will not be evicted.
     */
    private static final ConcurrentMap<String, String> resolvedAddresses;

    static {
        Cache<InetSocketAddress, HttpConnectTiming> connectTimingCache = CacheBuilder.newBuilder()
                .expireAfterWrite(HTTP_CONNECT_TIMING_EVICTION_SECONDS, TimeUnit.SECONDS)
                .concurrencyLevel(HTTP_CONNECT_TIMING_CONCURRENCY_LEVEL)
                .build();
        httpConnectTimes = connectTimingCache.asMap();

        Cache<String, String> addressCache = CacheBuilder.newBuilder()
                .expireAfterAccess(RESOLVED_ADDRESSES_EVICTION_SECONDS, TimeUnit.SECONDS)
                .concurrencyLevel(HTTP_CONNECT_TIMING_CONCURRENCY_LEVEL)
                .build();
        resolvedAddresses = addressCache.asMap();
    }

    /**
     * Create a new instance of the HarCaptureFilter that will capture request and response information. If no har is specified in the
     * constructor, this filter will do nothing.
     * <p/>
     * Regardless of the CaptureTypes specified in <code>dataToCapture</code>, the HarCaptureFilter will always capture:
     * <ul>
     *     <li>Request and response sizes</li>
     *     <li>HTTP request and status lines</li>
     *     <li>Page timing information</li>
     * </ul>
     *
     * @param originalRequest the original HttpRequest from the HttpFiltersSource factory
     * @param har a reference to the ProxyServer's current HAR file at the time this request is received (can be null if HAR capture is not required)
     * @param currentPageRef the ProxyServer's currentPageRef at the time this request is received from the client
     * @param dataToCapture the data types to capture for this request. null or empty set indicates only basic information will be
     *                      captured (see {@link net.lightbody.bmp.proxy.CaptureType} for information on data collected for each CaptureType)
     */
    public HarCaptureFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, Har har, String currentPageRef, Set<CaptureType> dataToCapture) {
        super(originalRequest, ctx);

        httpConnect = originalRequest.getMethod().equals(HttpMethod.CONNECT);

        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        this.clientAddress = clientAddress;

        // for HTTP CONNECT calls, create and cache an HTTP CONNECT timing object to capture timing-related information
        if (httpConnect) {
            this.httpConnectTiming = new HttpConnectTiming();
            httpConnectTimes.put(clientAddress, httpConnectTiming);
        } else {
            httpConnectTiming = null;
        }

        if (har == null || httpConnect) {
            // if har capture is disabled, this filter is a no-op. for HTTP CONNECT requests we still need to capture some basic timing
            // information, but no HarEntry will be added to the HarLog.
            this.harEntry = null;
            this.requestCaptureFilter = null;
            this.responseCaptureFilter = null;
            this.dataToCapture = null;
            this.har = null;
        } else {
            if (dataToCapture != null && !dataToCapture.isEmpty()) {
                this.dataToCapture = EnumSet.copyOf(dataToCapture);
            } else {
                this.dataToCapture = EnumSet.noneOf(CaptureType.class);
            }

            // we may need to capture both the request and the response, so set up the request/response filters and delegate to them when
            // the corresponding filter methods are invoked. to save time and memory, only set up the capturing filters when
            // we actually need to capture the data.
            if (dataToCapture.contains(CaptureType.REQUEST_CONTENT) || dataToCapture.contains(CaptureType.REQUEST_BINARY_CONTENT)) {
                requestCaptureFilter = new ClientRequestCaptureFilter(originalRequest);
            } else {
                requestCaptureFilter = null;
            }

            if (dataToCapture.contains(CaptureType.RESPONSE_CONTENT) || dataToCapture.contains(CaptureType.RESPONSE_BINARY_CONTENT)) {
                responseCaptureFilter = new ServerResponseCaptureFilter(originalRequest, true);
            } else {
                responseCaptureFilter = null;
            }

            this.har = har;

            this.harEntry = new HarEntry(currentPageRef);
        }
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (har == null) {
            return null;
        }

        // if a ServerResponseCaptureFilter is configured, delegate to it to collect the client request. if it is not
        // configured, we still need to capture basic information (timings, possibly client headers, etc.), just not content.
        if (requestCaptureFilter != null) {
            requestCaptureFilter.clientToProxyRequest(httpObject);
        }

        if (httpObject instanceof HttpRequest) {
            // link the object up now, before we make the request, so that if we get cut off (ie: favicon.ico request and browser shuts down)
            // we still have the attempt associated, even if we never got a response
            harEntry.setStartedDateTime(new Date());
            har.getLog().addEntry(harEntry);

            HttpRequest httpRequest = (HttpRequest) httpObject;
            this.capturedOriginalRequest = httpRequest;

            captureRequestUrl(httpRequest);
            captureUserAgent(httpRequest);
            captureRequestHeaderSize(httpRequest);

            if (dataToCapture.contains(CaptureType.REQUEST_COOKIES)) {
                captureRequestCookies(httpRequest);
            }

            if (dataToCapture.contains(CaptureType.REQUEST_HEADERS)) {
                captureRequestHeaders(httpRequest);
            }

            // The HTTP CONNECT to the proxy server establishes the SSL connection to the remote server, but the HTTP CONNECT is not recorded in
            // a separate HarEntry. Instead, the ssl and connect times are recorded in the first request between the client and remote server
            // after the HTTP CONNECT.
            captureConnectTiming();
        }

        if (httpObject instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) httpObject;

            captureRequestSize(httpContent);
        }

        if (httpObject instanceof LastHttpContent) {
            LastHttpContent lastHttpContent = (LastHttpContent) httpObject;
            if (dataToCapture.contains(CaptureType.REQUEST_HEADERS)) {
                captureTrailingHeaders(lastHttpContent);
            }

            if (dataToCapture.contains(CaptureType.REQUEST_CONTENT)) {
                captureRequestContent(requestCaptureFilter.getHttpRequest(), requestCaptureFilter.getFullRequestContents());
            }

            harEntry.getRequest().setBodySize(requestBodySize.get());
        }

        return null;
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        if (har == null) {
            return super.serverToProxyResponse(httpObject);
        }

        // if a ServerResponseCaptureFilter is configured, delegate to it to collect the server's response. if it is not
        // configured, we still need to capture basic information (timings, HTTP status, etc.), just not content.
        if (responseCaptureFilter != null) {
            responseCaptureFilter.serverToProxyResponse(httpObject);
        }

        if (httpObject instanceof HttpResponse) {
            HttpResponse httpResponse = (HttpResponse) httpObject;

            captureResponse(httpResponse);
        }

        if (httpObject instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) httpObject;

            captureResponseSize(httpContent);
        }

        if (httpObject instanceof LastHttpContent) {
            if (dataToCapture.contains(CaptureType.RESPONSE_CONTENT)) {
                captureResponseContent(responseCaptureFilter.getHttpResponse(), responseCaptureFilter.getFullResponseContents());
            }

            harEntry.getResponse().setBodySize(responseBodySize.get());
        }

        return super.serverToProxyResponse(httpObject);
    }

    protected void captureRequestUrl(HttpRequest httpRequest) {
        HarRequest request = new HarRequest(httpRequest.getMethod().toString(), httpRequest.getUri(), httpRequest.getProtocolVersion().text());
        harEntry.setRequest(request);

        // capture query parameters
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.getUri());
        for (Map.Entry<String, List<String>> entry : queryStringDecoder.parameters().entrySet()) {
         for (String value : entry.getValue()) {
             harEntry.getRequest().getQueryString().add(new HarNameValuePair(entry.getKey(), value));
         }
        }
     }

    protected void captureUserAgent(HttpRequest httpRequest) {
        // save the browser and version if it's not yet been set
        if (har.getLog().getBrowser() == null) {
            String userAgentHeader = HttpHeaders.getHeader(httpRequest, HttpHeaders.Names.USER_AGENT);
            if (userAgentHeader != null && userAgentHeader.length() > 0) {
                try {
                    ReadableUserAgent uai = BrowserMobProxyUtil.getUserAgentStringParser().parse(userAgentHeader);
                    String browser = uai.getName();
                    String version = uai.getVersionNumber().toVersionString();
                    har.getLog().setBrowser(new HarNameVersion(browser, version));
                } catch (RuntimeException e) {
                    log.warn("Failed to parse user agent string", e);
                }
            }
        }
    }

    protected void captureRequestHeaderSize(HttpRequest httpRequest) {
        String requestLine = httpRequest.getMethod().toString() + ' ' + httpRequest.getUri().toString() + ' ' + httpRequest.getProtocolVersion().toString();
        // +2 => CRLF after status line, +4 => header/data separation
        long requestHeadersSize = requestLine.length() + 6;

        HttpHeaders headers = httpRequest.headers();
        requestHeadersSize += BrowserMobHttpUtil.getHeaderSize(headers);

        harEntry.getRequest().setHeadersSize(requestHeadersSize);
    }

    protected void captureRequestCookies(HttpRequest httpRequest) {
        String cookieHeader = httpRequest.headers().get(HttpHeaders.Names.COOKIE);
        if (cookieHeader == null) {
            return;
        }

        Set<Cookie> cookies = CookieDecoder.decode(cookieHeader);

        for (Cookie cookie : cookies) {
            HarCookie harCookie = new HarCookie();

            harCookie.setName(cookie.getName());
            harCookie.setValue(cookie.getValue());

            harEntry.getRequest().getCookies().add(harCookie);
        }
    }

    protected void captureRequestHeaders(HttpRequest httpRequest) {
        HttpHeaders headers = httpRequest.headers();

        captureHeaders(headers);
    }

    protected void captureTrailingHeaders(LastHttpContent lastHttpContent) {
        HttpHeaders headers = lastHttpContent.trailingHeaders();

        captureHeaders(headers);
    }

    private void captureHeaders(HttpHeaders headers) {
        for (Map.Entry<String, String> header : headers.entries()) {
            harEntry.getRequest().getHeaders().add(new HarNameValuePair(header.getKey(), header.getValue()));
        }
    }

    protected void captureRequestContent(HttpRequest httpRequest, byte[] fullMessage) {
        if (fullMessage.length == 0) {
            return;
        }

        String contentType = HttpHeaders.getHeader(httpRequest, HttpHeaders.Names.CONTENT_TYPE);
        if (contentType == null) {
            log.warn("No content type specified in request to {}. Content will be treated as {}", httpRequest.getUri(), BrowserMobHttpUtil.UNKNOWN_CONTENT_TYPE);
            contentType = BrowserMobHttpUtil.UNKNOWN_CONTENT_TYPE;
        }

        HarPostData postData = new HarPostData();
        harEntry.getRequest().setPostData(postData);

        postData.setMimeType(contentType);

        boolean urlEncoded;
        if (contentType.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
            urlEncoded = true;
        } else {
            urlEncoded = false;
        }

        if (urlEncoded) {
            String textContents = BrowserMobHttpUtil.getContentAsString(fullMessage, contentType, originalRequest);
            Charset charset = BrowserMobHttpUtil.deriveCharsetFromContentTypeHeader(contentType);

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(textContents, charset, false);
            for (Map.Entry<String, List<String>> entry : queryStringDecoder.parameters().entrySet()) {
                List<HarPostDataParam> params = new ArrayList<HarPostDataParam>();
                harEntry.getRequest().getPostData().setParams(params);

                for (String value : entry.getValue()) {
                    params.add(new HarPostDataParam(entry.getKey(), value));
                }
            }
        } else {
            //TODO: implement capture of files and multipart form data

            // not URL encoded, so let's grab the body of the POST and capture that
            String postBody = BrowserMobHttpUtil.getContentAsString(fullMessage, contentType, originalRequest);
            harEntry.getRequest().getPostData().setText(postBody);
        }
    }

    protected void captureResponseContent(HttpResponse httpResponse, byte[] fullMessage) {
        // force binary if the content encoding is not supported
        boolean forceBinary = false;

        String contentType = HttpHeaders.getHeader(httpResponse, HttpHeaders.Names.CONTENT_TYPE);
        if (contentType == null) {
            log.warn("No content type specified in response. Content will be treated as {}", BrowserMobHttpUtil.UNKNOWN_CONTENT_TYPE);
            contentType = BrowserMobHttpUtil.UNKNOWN_CONTENT_TYPE;
        }

        harEntry.getResponse().getContent().setMimeType(contentType);

        if (responseCaptureFilter.isResponseCompressed() && !responseCaptureFilter.isDecompressionSuccessful()) {
            log.warn("Unable to decompress content with encoding: {}. Contents will be encoded as base64 binary data.", responseCaptureFilter.getContentEncoding());

            forceBinary = true;
        }

        if (!forceBinary && BrowserMobHttpUtil.hasTextualContent(contentType)) {
            String text = BrowserMobHttpUtil.getContentAsString(fullMessage, contentType, originalRequest);
            harEntry.getResponse().getContent().setText(text);
        } else if (dataToCapture.contains(CaptureType.RESPONSE_BINARY_CONTENT)) {
            harEntry.getResponse().getContent().setText(DatatypeConverter.printBase64Binary(fullMessage));
            harEntry.getResponse().getContent().setEncoding("base64");
        }
    }

    protected void captureResponse(HttpResponse httpResponse) {
        HarResponse response = new HarResponse(httpResponse.getStatus().code(), httpResponse.getStatus().reasonPhrase(), httpResponse.getProtocolVersion().text());
        harEntry.setResponse(response);

        captureResponseHeaderSize(httpResponse);

        if (dataToCapture.contains(CaptureType.RESPONSE_COOKIES)) {
            captureResponseCookies(httpResponse);
        }

        if (dataToCapture.contains(CaptureType.RESPONSE_HEADERS)) {
            captureResponseHeaders(httpResponse);
        }
    }

    protected void captureResponseCookies(HttpResponse httpResponse) {
        List<String> setCookieHeaders = httpResponse.headers().getAll(HttpHeaders.Names.SET_COOKIE);
        if (setCookieHeaders == null) {
            return;
        }

        for (String setCookieHeader : setCookieHeaders) {
            Set<Cookie> cookies = CookieDecoder.decode(setCookieHeader);
            // really there should only be one cookie per Set-Cookie header
            for (Cookie cookie : cookies) {
                HarCookie harCookie = new HarCookie();

                harCookie.setName(cookie.getName());
                harCookie.setValue(cookie.getValue());
                harCookie.setComment(cookie.getComment());
                harCookie.setDomain(cookie.getDomain());
                harCookie.setHttpOnly(cookie.isHttpOnly());
                harCookie.setPath(cookie.getPath());
                harCookie.setSecure(cookie.isSecure());
                harCookie.setExpires(new Date(System.currentTimeMillis() + cookie.getMaxAge()));

                harEntry.getResponse().getCookies().add(harCookie);
            }
        }
    }

    protected void captureResponseHeaderSize(HttpResponse httpResponse) {
        String statusLine = httpResponse.getProtocolVersion().toString() + ' ' + httpResponse.getStatus().toString();
        // +2 => CRLF after status line, +4 => header/data separation
        long responseHeadersSize = statusLine.length() + 6;
        HttpHeaders headers = httpResponse.headers();
        responseHeadersSize += BrowserMobHttpUtil.getHeaderSize(headers);

        harEntry.getResponse().setHeadersSize(responseHeadersSize);
    }

    protected void captureResponseHeaders(HttpResponse httpResponse) {
        HttpHeaders headers = httpResponse.headers();
        for (Map.Entry<String, String> header : headers.entries()) {
            harEntry.getResponse().getHeaders().add(new HarNameValuePair(header.getKey(), header.getValue()));
        }
    }

    /**
     * Adds the size of this httpContent to the requestBodySize.
     *
     * @param httpContent HttpContent to size
     */
    protected void captureRequestSize(HttpContent httpContent) {
        ByteBuf bufferedContent = httpContent.content();
        int contentSize = bufferedContent.readableBytes();
        requestBodySize.addAndGet(contentSize);
    }

    /**
     * Adds the size of this httpContent to the responseBodySize.
     *
     * @param httpContent HttpContent to size
     */
    protected void captureResponseSize(HttpContent httpContent) {
        ByteBuf bufferedContent = httpContent.content();
        int contentSize = bufferedContent.readableBytes();
        responseBodySize.addAndGet(contentSize);
    }

    /**
     * Populates ssl and connect timing info in the HAR if an entry for this client and server exist in the httpConnectTimes map.
     */
    protected void captureConnectTiming() {
        HttpConnectTiming httpConnectTiming = httpConnectTimes.remove(clientAddress);
        if (httpConnectTiming != null) {
            harEntry.getTimings().setSsl(httpConnectTiming.getSslHandshakeTimeNanos(), TimeUnit.NANOSECONDS);
            harEntry.getTimings().setConnect(httpConnectTiming.getConnectTimeNanos(), TimeUnit.NANOSECONDS);
            harEntry.getTimings().setBlocked(httpConnectTiming.getBlockedTimeNanos(), TimeUnit.NANOSECONDS);
            harEntry.getTimings().setDns(httpConnectTiming.getDnsTimeNanos(), TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Populates the serverIpAddress field of the harEntry using the internal hostname->IP address cache.
     *
     * @param httpRequest HTTP request to take the hostname from
     */
    protected void populateAddressFromCache(HttpRequest httpRequest) {
        String serverHost = BrowserMobHttpUtil.identifyHostFromRequest(httpRequest);
        if (serverHost != null && !serverHost.isEmpty()) {
            String resolvedAddress = resolvedAddresses.get(serverHost);
            if (resolvedAddress != null) {
                harEntry.setServerIPAddress(resolvedAddress);
            } else {
                log.warn("Unable to find cached IP address for host: {}. IP address in HAR entry will be blank.", serverHost);
            }
        } else {
            log.warn("Unable to identify host from request uri: {}", httpRequest.getUri());
        }
    }

    @Override
    public InetSocketAddress proxyToServerResolutionStarted(String resolvingServerHostAndPort) {
        if (har == null && !httpConnect) {
            return null;
        }

        dnsResolutionStartedNanos = System.nanoTime();

        if (httpConnect) {
            httpConnectTiming.setBlockedTimeNanos(dnsResolutionStartedNanos - connectionQueuedNanos);
        } else {
            // resolution started means the connection is no longer queued, so populate 'blocked' time
            harEntry.getTimings().setBlocked(dnsResolutionStartedNanos - connectionQueuedNanos, TimeUnit.NANOSECONDS);
        }

        return null;
    }

    @Override
    public void proxyToServerResolutionSucceeded(String serverHostAndPort, InetSocketAddress resolvedRemoteAddress) {
        if (har == null && !httpConnect) {
            return;
        }

        long dnsResolutionFinishedNanos = System.nanoTime();

        if (httpConnect) {
            httpConnectTiming.setDnsTimeNanos(dnsResolutionFinishedNanos - dnsResolutionStartedNanos);
        } else {
            harEntry.getTimings().setDns(dnsResolutionFinishedNanos - dnsResolutionStartedNanos, TimeUnit.NANOSECONDS);
        }

        // the address *should* always be resolved at this point
        InetAddress resolvedAddress = resolvedRemoteAddress.getAddress();
        if (resolvedAddress != null) {
            addressResolved = true;

            if (har != null) {
                harEntry.setServerIPAddress(resolvedAddress.getHostAddress());
            }

            // place the resolved host into the hostname cache, so subsequent requests will be able to identify the IP address
            HostAndPort parsedHostAndPort = HostAndPort.fromString(serverHostAndPort);
            String host = parsedHostAndPort.getHostText();

            if (host != null && !host.isEmpty()) {
                resolvedAddresses.put(host, resolvedAddress.getHostAddress());
            }
        }

        return;
    }

    @Override
    public void proxyToServerConnectionQueued() {
        this.connectionQueuedNanos = System.nanoTime();
    }

    @Override
    public void proxyToServerConnectionStarted() {
        this.connectionStartedNanos = System.nanoTime();
    }

    @Override
    public void proxyToServerConnectionSSLHandshakeStarted() {
        this.sslHandshakeStartedNanos = System.nanoTime();
    }

    @Override
    public void proxyToServerConnectionSucceeded() {
        if (har == null && !httpConnect) {
            return;
        }

        long connectionSucceededTimeNanos = System.nanoTime();

        if (httpConnect) {
            // store SSL timing information in the global map so the subsequent HTTP request from the client can capture ssl and connect timing info
            httpConnectTiming.setConnectTimeNanos(connectionSucceededTimeNanos - this.connectionStartedNanos);
            httpConnectTiming.setSslHandshakeTimeNanos(connectionSucceededTimeNanos - this.sslHandshakeStartedNanos);
        } else {
            harEntry.getTimings().setConnect(connectionSucceededTimeNanos - connectionStartedNanos, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void proxyToServerRequestSending() {
        this.sendStartedNanos = System.nanoTime();

        // if the hostname was not resolved (and thus the IP address populated in the har) during this request, populate the IP address from the cache
        if (har != null && !addressResolved) {
            populateAddressFromCache(capturedOriginalRequest);
        }
    }

    @Override
    public void proxyToServerRequestSent() {
        if (har == null) {
            return;
        }

        this.sendFinishedNanos = System.nanoTime();

        harEntry.getTimings().setSend(sendFinishedNanos - sendStartedNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void serverToProxyResponseReceiving() {
        if (har == null) {
            return;
        }

        this.responseReceiveStartedNanos = System.nanoTime();

        // started to receive response, so populate the 'wait' time
        harEntry.getTimings().setWait(responseReceiveStartedNanos - sendFinishedNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void serverToProxyResponseReceived() {
        if (har == null) {
            return;
        }

        long responseReceivedNanos = System.nanoTime();

        harEntry.getTimings().setReceive(responseReceivedNanos - responseReceiveStartedNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Holds the connection-related timing information from an HTTP CONNECT request, so it can be added to the HAR timings for the first
     * "real" request to the same host. The HTTP CONNECT and the "real" HTTP requests are processed in different HarCaptureFilter instances.
     * <p/>
     * <b>Note:</b> The connect time must include the ssl time. According to the HAR spec at <a href="https://dvcs.w3.org/hg/webperf/raw-file/tip/specs/HAR/Overview.htm">https://dvcs.w3.org/hg/webperf/raw-file/tip/specs/HAR/Overview.htm</a>:
     <pre>
     ssl [number, optional] (new in 1.2) - Time required for SSL/TLS negotiation. If this field is defined then the time is also
     included in the connect field (to ensure backward compatibility with HAR 1.1). Use -1 if the timing does not apply to the
     current request.
     </pre>
     */
    private static class HttpConnectTiming {
        private volatile long connectTimeNanos;
        private volatile long sslHandshakeTimeNanos;
        private volatile long blockedTimeNanos;
        private volatile long dnsTimeNanos;

        public void setConnectTimeNanos(long connectTimeNanos) {
            this.connectTimeNanos = connectTimeNanos;
        }

        public void setSslHandshakeTimeNanos(long sslHandshakeTimeNanos) {
            this.sslHandshakeTimeNanos = sslHandshakeTimeNanos;
        }

        public void setBlockedTimeNanos(long blockedTimeNanos) {
            this.blockedTimeNanos = blockedTimeNanos;
        }

        public void setDnsTimeNanos(long dnsTimeNanos) {
            this.dnsTimeNanos = dnsTimeNanos;
        }

        public long getConnectTimeNanos() {
            return connectTimeNanos;
        }

        public long getSslHandshakeTimeNanos() {
            return sslHandshakeTimeNanos;
        }

        public long getBlockedTimeNanos() {
            return blockedTimeNanos;
        }

        public long getDnsTimeNanos() {
            return dnsTimeNanos;
        }
    }

}
