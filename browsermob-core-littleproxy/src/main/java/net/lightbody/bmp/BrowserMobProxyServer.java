package net.lightbody.bmp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.net.HostAndPort;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarNameVersion;
import net.lightbody.bmp.core.har.HarPage;
import net.lightbody.bmp.exception.NameResolutionException;
import net.lightbody.bmp.filters.AddHeadersFilter;
import net.lightbody.bmp.filters.BlacklistFilter;
import net.lightbody.bmp.filters.BrowserMobHttpFilterChain;
import net.lightbody.bmp.filters.HarCaptureFilter;
import net.lightbody.bmp.filters.HttpsConnectHarCaptureFilter;
import net.lightbody.bmp.filters.HttpsHostCaptureFilter;
import net.lightbody.bmp.filters.HttpsOriginalHostCaptureFilter;
import net.lightbody.bmp.filters.LatencyFilter;
import net.lightbody.bmp.filters.RegisterRequestFilter;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.RequestFilterAdapter;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.filters.ResponseFilterAdapter;
import net.lightbody.bmp.filters.RewriteUrlFilter;
import net.lightbody.bmp.filters.UnregisterRequestFilter;
import net.lightbody.bmp.filters.WhitelistFilter;
import net.lightbody.bmp.proxy.ActivityMonitor;
import net.lightbody.bmp.proxy.BlacklistEntry;
import net.lightbody.bmp.proxy.CaptureType;
import net.lightbody.bmp.proxy.LegacyProxyServer;
import net.lightbody.bmp.proxy.RewriteRule;
import net.lightbody.bmp.proxy.Whitelist;
import net.lightbody.bmp.proxy.auth.AuthType;
import net.lightbody.bmp.proxy.dns.AdvancedHostResolver;
import net.lightbody.bmp.proxy.dns.DelegatingHostResolver;
import net.lightbody.bmp.proxy.http.RequestInterceptor;
import net.lightbody.bmp.proxy.http.ResponseInterceptor;
import net.lightbody.bmp.proxy.util.BrowserMobProxyUtil;
import net.lightbody.bmp.ssl.BrowserMobProxyMitmManager;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.java_bandwidthlimiter.StreamManager;
import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.openqa.selenium.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * A LittleProxy-based implementation of {@link net.lightbody.bmp.BrowserMobProxy}.
 */
public class BrowserMobProxyServer implements BrowserMobProxy, LegacyProxyServer {
    private static final Logger log = LoggerFactory.getLogger(BrowserMobProxyServer.class);

    private static final HarNameVersion HAR_CREATOR_VERSION = new HarNameVersion("BrowserMob Proxy", "2.1.0-beta-1-littleproxy");

    /**
     * True only after the proxy has been successfully started.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * True only after the proxy has been successfully started, then successfully stopped or aborted.
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Tracks the current page count, for use when auto-generating HAR page names.
     */
    private final AtomicInteger harPageCount = new AtomicInteger(0);

    /**
     * When true, MITM will be disabled. The proxy will no longer intercept HTTPS requests, but they will still be proxied.
     */
    private volatile boolean mitmDisabled = false;

    /**
     * The list of filterFactories that will generate the filters that implement browsermob-proxy behavior.
     */
    private final List<HttpFiltersSource> filterFactories = new CopyOnWriteArrayList<HttpFiltersSource>();

    /**
     * List of rejected URL patterns
     */
    private volatile Collection<BlacklistEntry> blacklistEntries = new CopyOnWriteArrayList<BlacklistEntry>();

    /**
     * List of URLs to rewrite
     */
    private volatile CopyOnWriteArrayList<RewriteRule> rewriteRules = new CopyOnWriteArrayList<RewriteRule>();

    /**
     * The LittleProxy instance that performs all proxy operations.
     */
    private volatile HttpProxyServer proxyServer;

    /**
     * The proxy port set using the legacy {@link #setPort(int)} method.
     */
    private volatile int port = 0;

    /**
     * Cookie capture is on by default, if HAR capture is enabled.
     * TODO: determine if this is the behavior we want in the future
     */
    private volatile EnumSet<CaptureType> harCaptureTypes = CaptureType.getCookieCaptureTypes();

    /**
     * The current HAR being captured.
     */
    private volatile Har har;
    /**
     * The current HarPage to which new requests will be associated.
     */
    private volatile HarPage currentHarPage;
    /**
     * Maximum bandwidth to consume when reading responses from servers.
     */
    private volatile long readBandwidthLimitBps;
    /**
     * Maximum bandwidth to consume when writing requests to servers.
     */
    private volatile long writeBandwidthLimitBps;
    /**
     * List of accepted URL patterns. Unlisted URL patterns will be rejected with the response code contained in the Whitelist.
     */
    private final AtomicReference<Whitelist> whitelist = new AtomicReference<>(Whitelist.WHITELIST_DISABLED);

    /**
     * Additional headers that will be sent with every request. The map is declared as a ConcurrentMap to indicate that writes may be performed
     * by other threads concurrently (e.g. due to an incoming REST call), but the concurrencyLevel is set to 1 because modifications to the
     * additionalHeaders are rare, and in most cases happen only once, at start-up.
     */
    private volatile ConcurrentMap<String, String> additionalHeaders = new MapMaker().concurrencyLevel(1).makeMap();

    /**
     * The amount of time to wait while connecting to a server.
     */
    private volatile int connectTimeoutMs;

    /**
     * The amount of time a connection to a server can remain idle while receiving data from the server.
     */
    private volatile int idleConnectionTimeoutSec;

    /**
     * The amount of time to wait before forwarding the response to the client.
     */
    private volatile int latencyMs;

    /**
     * Set to true once the HAR capture filter has been added to the filter chain.
     */
    private final AtomicBoolean harCaptureFilterEnabled = new AtomicBoolean(false);

    /**
     * The address of an upstream chained proxy to route traffic through.
     */
    private volatile InetSocketAddress upstreamProxyAddress;

    /**
     * The address and socket on which the proxy will listen for client requests.
     */
    private volatile InetSocketAddress clientBindSocket;

    /**
     * The address of the network interface from which the proxy will initiate connections.
     */
    private volatile InetAddress serverBindAddress;

    /**
     * Indicates that the legacy setLocalHost() method was used, so start() should use this.clientBindSocket.
     * Will be removed in a future release.
     */
    private volatile boolean legacyClientBindSocketSet;

    /**
     * When true, throw an UnsupportedOperationException instead of logging a warning when an operation is not supported by the LittleProxy-based
     * implementation of the BrowserMobProxy interface. Once all operations are implemented and the legacy interface is retired, this will be removed.
     */
    private volatile boolean errorOnUnsupportedOperation = false;

    /**
     * Resolver to use when resolving hostnames to IP addresses. This is a bridge between {@link org.littleshoot.proxy.HostResolver} and
     * {@link net.lightbody.bmp.proxy.dns.AdvancedHostResolver}. It allows the resolvers to be changed on-the-fly without re-bootstrapping the
     * littleproxy server. The default resolver (native JDK resolver) can be changed using {@link #setHostNameResolver(net.lightbody.bmp.proxy.dns.AdvancedHostResolver)} and
     * supplying one of the pre-defined resolvers in {@link ClientUtil}, such as {@link ClientUtil#createDnsJavaWithNativeFallbackResolver()}
     * or {@link ClientUtil#createDnsJavaResolver()}. You can also build your own resolver, or use {@link net.lightbody.bmp.proxy.dns.ChainedHostResolver}
     * to chain together multiple DNS resolvers.
     */
    private final DelegatingHostResolver delegatingResolver = new DelegatingHostResolver(ClientUtil.createNativeCacheManipulatingResolver());

    private final ActivityMonitor activityMonitor = new ActivityMonitor();

    /**
     * Adapter to enable clients to switch to a LittleProxy implementation of BrowserMobProxy but maintain compatibility with
     * the 2.0.0 interface.
     */
    @Deprecated
    private class StreamManagerLegacyAdapter extends StreamManager {
        private StreamManagerLegacyAdapter() {
            super(0);
        }

        @Override
        public void setDownstreamKbps(long downstreamKbps) {
            BrowserMobProxyServer.this.setDownstreamKbps(downstreamKbps);
        }

        @Override
        public void setUpstreamKbps(long upstreamKbps) {
            BrowserMobProxyServer.this.setUpstreamKbps(upstreamKbps);
        }

        @Override
        public void setLatency(long latency) {
            BrowserMobProxyServer.this.setLatency(latency);
        }

        @Override
        public void setDownstreamMaxKB(long downstreamMaxKB) {
            BrowserMobProxyServer.this.setWriteLimitKbps(downstreamMaxKB);
        }

        @Override
        public void setUpstreamMaxKB(long upstreamMaxKB) {
            BrowserMobProxyServer.this.setReadLimitKbps(upstreamMaxKB);
        }
    }

    /**
     * StreamManagerLegacyAdapter bound to this BrowserMob Proxy instance, to route the bandwidth control calls to the appropriate
     * LittleProxy-compatible methods.
     */
    private final StreamManagerLegacyAdapter streamManagerAdapter = new StreamManagerLegacyAdapter();

    public BrowserMobProxyServer() {
        this(0);
    }

    public BrowserMobProxyServer(int port) {
        this.port = port;
    }

    @Override
    public void start(int port, InetAddress clientBindAddress, InetAddress serverBindAddress) {
        boolean notStarted = started.compareAndSet(false, true);
        if (!notStarted) {
            throw new IllegalStateException("Proxy server is already started. Not restarting.");
        }

        if (legacyClientBindSocketSet) {
            clientBindSocket = new InetSocketAddress(clientBindSocket.getAddress(), port);
        } else {
            if (clientBindAddress == null) {
                // if no client bind address was specified, bind to the wildcard address
                clientBindSocket = new InetSocketAddress(port);
            } else {
                clientBindSocket = new InetSocketAddress(clientBindAddress, port);
            }
        }

        this.serverBindAddress = serverBindAddress;

        // initialize all the default BrowserMob filter factories that provide core BMP functionality
        addBrowserMobFilters();

        HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap()
                .withFiltersSource(new HttpFiltersSource() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext channelHandlerContext) {
                        return new BrowserMobHttpFilterChain(BrowserMobProxyServer.this, originalRequest, channelHandlerContext);
                    }

                    @Override
                    public int getMaximumRequestBufferSizeInBytes() {
                        return getMaximumRequestBufferSize();
                    }

                    @Override
                    public int getMaximumResponseBufferSizeInBytes() {
                        return getMaximumResponseBufferSize();
                    }
                })
                .withServerResolver(delegatingResolver)
                .withAddress(clientBindSocket)
                .withConnectTimeout(connectTimeoutMs)
                .withIdleConnectionTimeout(idleConnectionTimeoutSec);

        if (serverBindAddress != null) {
            bootstrap.withNetworkInterface(new InetSocketAddress(serverBindAddress, 0));
        }


        if (!mitmDisabled) {
            bootstrap.withManInTheMiddle(new BrowserMobProxyMitmManager());
        }

        if (readBandwidthLimitBps > 0 || writeBandwidthLimitBps > 0) {
            bootstrap.withThrottling(readBandwidthLimitBps, writeBandwidthLimitBps);
        }

        if (upstreamProxyAddress != null) {
            bootstrap.withChainProxyManager(new ChainedProxyManager() {
                @Override
                public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies) {
                    chainedProxies.add(new ChainedProxyAdapter() {
                        @Override
                        public InetSocketAddress getChainedProxyAddress() {
                            return upstreamProxyAddress;
                        }
                    });
                }
            });
        }

        proxyServer = bootstrap.start();
    }

    @Override
    public boolean isStarted() {
        return started.get();
    }

    @Override
    public void start(int port) {
        this.start(port, null, null);
    }

    @Override
    public void start(int port, InetAddress bindAddress) {
        this.start(port, bindAddress, null);

    }

    @Override
    public void start() {
        this.start(port);
    }

    /**
     * @deprecated use {@link net.lightbody.bmp.client.ClientUtil#createSeleniumProxy(BrowserMobProxy)}
     */
    @Override
    @Deprecated
    public Proxy seleniumProxy() throws NameResolutionException {
        return ClientUtil.createSeleniumProxy(this);
    }

    @Override
    public void stop() {
        stop(true);
    }

    @Override
    public void abort() {
        stop(false);
    }

    protected void stop(boolean graceful) {
        if (isStarted()) {
            if (stopped.compareAndSet(false, true)) {
                if (proxyServer != null) {
                    if (graceful) {
                        proxyServer.stop();
                    } else {
                        proxyServer.abort();
                    }
                } else {
                    log.warn("Attempted to stop proxy server, but proxy was never successfully started.");
                }
            } else {
                throw new IllegalStateException("Proxy server is already stopped. Cannot re-stop.");
            }
        } else {
            throw new IllegalStateException("Proxy server has not been started");
        }
    }

    @Override
    public InetAddress getClientBindAddress() {
        if (clientBindSocket == null) {
            return null;
        }

        return clientBindSocket.getAddress();
    }

    /**
     * @deprecated this method has no effect and will be removed from a future version
     */
    @Deprecated
    public void cleanup() {
        //TODO: log warning when calling deprecated code?
    }

    @Override
    public int getPort() {
        if (started.get()) {
            return proxyServer.getListenAddress().getPort();
        } else {
            return this.port;
        }
    }

    /**
     * @deprecated specify the port using {@link #start(int)} or other start() methods with port parameters
     */
    @Override
    @Deprecated
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @deprecated use {@link #getClientBindAddress()}
     */
    @Override
    @Deprecated
    public InetAddress getLocalHost() {
        return this.getClientBindAddress();
    }

    /**
     * @deprecated use {@link net.lightbody.bmp.client.ClientUtil#getConnectableAddress()}
     */
    @Override
    @Deprecated
    public InetAddress getConnectableLocalHost() throws UnknownHostException {
        return ClientUtil.getConnectableAddress();
    }

    /**
     * @deprecated use {@link #start(int, java.net.InetAddress)} or {@link #start(int, java.net.InetAddress, java.net.InetAddress)}
     */
    @Override
    @Deprecated
    public void setLocalHost(InetAddress localHost) {
        legacyClientBindSocketSet = true;
        this.clientBindSocket = new InetSocketAddress(localHost, 0);
    }

    @Override
    public InetAddress getServerBindAddress() {
        return serverBindAddress;
    }

    @Override
    public Har getHar() {
        return har;
    }

    @Override
    public Har newHar() {
        return newHar(null);
    }

    @Override
    public Har newHar(String initialPageRef) {
        return newHar(initialPageRef, null);
    }

    @Override
    public Har newHar(String initialPageRef, String initialPageTitle) {
        // eagerly initialize the User Agent String Parser, since it will be needed for the HAR
        BrowserMobProxyUtil.getUserAgentStringParser();

        Har oldHar = getHar();

        addHarCaptureFilter();

        harPageCount.set(0);

        this.har = new Har(new HarLog(HAR_CREATOR_VERSION));

        newPage(initialPageRef, initialPageTitle);

        return oldHar;
    }

    @Override
    public void setHarCaptureTypes(Set<CaptureType> harCaptureSettings) {
        harCaptureTypes = EnumSet.copyOf(harCaptureSettings);
    }

    @Override
    public EnumSet<CaptureType> getHarCaptureTypes() {
        return EnumSet.copyOf(harCaptureTypes);
    }

    @Override
    public void enableHarCaptureTypes(Set<CaptureType> captureTypes) {
        harCaptureTypes.addAll(captureTypes);
    }

    @Override
    public void disableHarCaptureTypes(Set<CaptureType> captureTypes) {
        harCaptureTypes.removeAll(captureTypes);

    }

    @Override
    public Har newPage() {
        return newPage(null);
    }

    @Override
    public Har newPage(String pageRef) {
        return newPage(pageRef, null);
    }

    @Override
    public Har newPage(String pageRef, String pageTitle) {
        if (har == null) {
            throw new IllegalStateException("No HAR exists for this proxy. Use newHar() to create a new HAR before calling newPage().");
        }

        Har endOfPageHar = null;

        if (currentHarPage != null) {
            String currentPageRef = currentHarPage.getId();

            // end the previous page, so that page-wide timings are populated
            endPage();

            // the interface requires newPage() to return the Har as it was immediately after the previous page was ended.
            endOfPageHar = BrowserMobProxyUtil.copyHarThroughPageRef(har, currentPageRef);
        }

        if (pageRef == null) {
            pageRef = "Page " + harPageCount.getAndIncrement();
        }

        if (pageTitle == null) {
            pageTitle = pageRef;
        }

        HarPage newPage = new HarPage(pageRef, pageTitle);
        har.getLog().addPage(newPage);

        currentHarPage = newPage;

        return endOfPageHar;
    }

    @Override
    public Har endHar() {
        Har oldHar = getHar();

        // end the page and populate timings
        endPage();

        this.har = null;

        return oldHar;
    }

    @Override
    public void setReadBandwidthLimit(long bytesPerSecond) {
        this.readBandwidthLimitBps = bytesPerSecond;

        if (isStarted()) {
            proxyServer.setThrottle(this.readBandwidthLimitBps, this.writeBandwidthLimitBps);
        }

    }

    @Override
    public void setWriteBandwidthLimit(long bytesPerSecond) {
        this.writeBandwidthLimitBps = bytesPerSecond;

        if (isStarted()) {
            proxyServer.setThrottle(this.readBandwidthLimitBps, this.writeBandwidthLimitBps);
        }
    }

    @Override
    public void endPage() {
        if (har == null) {
            throw new IllegalStateException("No HAR exists for this proxy. Use newHar() to create a new HAR.");
        }

        HarPage previousPage = this.currentHarPage;
        this.currentHarPage = null;

        if (previousPage == null) {
            return;
        }

        previousPage.getPageTimings().setOnLoad(new Date().getTime() - previousPage.getStartedDateTime().getTime());
    }

    @Override
    public void setRetryCount(int count) {
        if (errorOnUnsupportedOperation) {
            throw new UnsupportedOperationException("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        } else {
            log.warn("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        }
    }

    @Override
    public void addHeaders(Map<String, String> headers) {
        ConcurrentMap<String, String> newHeaders = new MapMaker().concurrencyLevel(1).makeMap();
        newHeaders.putAll(headers);

        this.additionalHeaders = newHeaders;
    }

    /**
     * @deprecated Remap hosts directly using {@link net.lightbody.bmp.proxy.dns.AdvancedHostResolver#remapHost(String, String)}.
     * See {@link #getHostNameResolver()} and the default implementation in {@link net.lightbody.bmp.proxy.dns.ChainedHostResolver}.
     */
    @Override
    @Deprecated
    public void remapHost(String source, String target) {
        AdvancedHostResolver advancedResolver = delegatingResolver.getResolver();
        advancedResolver.remapHost(source, target);
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public void addRequestInterceptor(HttpRequestInterceptor i) {
        if (errorOnUnsupportedOperation) {
            throw new UnsupportedOperationException("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        } else {
            log.warn("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        }
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public void addRequestInterceptor(RequestInterceptor interceptor) {
        if (errorOnUnsupportedOperation) {
            throw new UnsupportedOperationException("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        } else {
            log.warn("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        }
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public void addResponseInterceptor(HttpResponseInterceptor i) {
        if (errorOnUnsupportedOperation) {
            throw new UnsupportedOperationException("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        } else {
            log.warn("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        }
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public void addResponseInterceptor(ResponseInterceptor interceptor) {
        if (errorOnUnsupportedOperation) {
            throw new UnsupportedOperationException("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        } else {
            log.warn("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        }
    }

    /**
     * This method returns a "fake" StreamManager whose setter methods will wrap LittleProxy-compatible bandwidth control methods. No other methods
     * in the returned StreamManager should be used; they will have no effect.
     *
     * @return fake StreamManager object that wraps LitteProxy-compatible bandwidth control methods
     * @deprecated use bandwidth control methods from the {@link net.lightbody.bmp.BrowserMobProxy}
     */
    @Deprecated
    public StreamManager getStreamManager() {
        return streamManagerAdapter;
    }

    /**
     * @deprecated use {@link #setWriteBandwidthLimit(long)}
     */
    @Deprecated
    public void setDownstreamKbps(long downstreamKbps) {
        this.setWriteBandwidthLimit(downstreamKbps * 1024);
    }

    /**
     * @deprecated use {@link #setReadBandwidthLimit(long)}
     */
    @Deprecated
    public void setUpstreamKbps(long upstreamKbps) {
        this.setReadBandwidthLimit(upstreamKbps * 1024);
    }

    /**
     * @deprecated use {@link #setLatency(long, java.util.concurrent.TimeUnit)}
     */
    @Deprecated
    public void setLatency(long latencyMs) {
        setLatency(latencyMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setLatency(long latency, TimeUnit timeUnit) {
        this.latencyMs = (int) TimeUnit.MILLISECONDS.convert(latency, timeUnit);
    }

    @Override
    public void autoAuthorization(String domain, String username, String password, AuthType authType) {
        if (errorOnUnsupportedOperation) {
            throw new UnsupportedOperationException("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        } else {
            log.warn("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        }
    }

    @Override
    public void stopAutoAuthorization(String domain) {
        if (errorOnUnsupportedOperation) {
            throw new UnsupportedOperationException("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        } else {
            log.warn("No LittleProxy implementation for operation: " + new Throwable().getStackTrace()[0].getMethodName());
        }
    }

    /**
     * @deprecated use {@link #setReadBandwidthLimit(long)}
     */
    @Deprecated
    public void setReadLimitKbps(long readLimitKbps) {
        setReadBandwidthLimit(readLimitKbps * 1024);
    }

    /**
     * @deprecated use {@link #setWriteBandwidthLimit(long)}
     */
    @Deprecated
    public void setWriteLimitKbps(long writeLimitKbps) {
        setWriteBandwidthLimit(writeLimitKbps * 1024);
    }

    @Override
    public void setConnectTimeout(int connectTimeout, TimeUnit timeUnit) {
        if (isStarted()) {
            throw new IllegalStateException("LittleProxy implementation does not allow changes to connect timeout after proxy has been started");
        }

        this.connectTimeoutMs = (int) TimeUnit.MILLISECONDS.convert(connectTimeout, timeUnit);
    }

    /**
     * The LittleProxy implementation only allows idle connection timeouts to be specified in seconds. idleConnectionTimeouts greater than
     * 0 but less than 1 second will be set to 1 second; otherwise, values will be truncated (i.e. 1500ms will become 1s).
     */
    @Override
    public void setIdleConnectionTimeout(int idleConnectionTimeout, TimeUnit timeUnit) {
        long timeout = TimeUnit.SECONDS.convert(idleConnectionTimeout, TimeUnit.MILLISECONDS);
        if (timeout == 0 && idleConnectionTimeout > 0) {
            this.idleConnectionTimeoutSec = 1;
        } else {
            this.idleConnectionTimeoutSec = (int) timeout;
        }

        if (isStarted()) {
            proxyServer.setIdleConnectionTimeout(idleConnectionTimeoutSec);
        }
    }

    @Override
    public void setRequestTimeout(int requestTimeout, TimeUnit timeUnit) {
        //TODO: implement Request Timeouts using LittleProxy. currently this only sets an idle connection timeout, if the idle connection
        // timeout is higher than the specified requestTimeout.
        if (idleConnectionTimeoutSec == 0 || idleConnectionTimeoutSec >  TimeUnit.SECONDS.convert(requestTimeout, timeUnit)) {
            setIdleConnectionTimeout(requestTimeout, timeUnit);
        }
    }

    /**
     * @deprecated use {@link #setConnectTimeout(int, java.util.concurrent.TimeUnit)}
     */
    @Deprecated
    @Override
    public void setConnectionTimeout(int connectionTimeoutMs) {
        setConnectTimeout(connectionTimeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * @deprecated use {@link #setIdleConnectionTimeout(int, java.util.concurrent.TimeUnit)}
     */
    @Deprecated
    @Override
    public void setSocketOperationTimeout(int readTimeoutMs) {
        setIdleConnectionTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * @deprecated use {@link #setRequestTimeout(int, java.util.concurrent.TimeUnit)}
     */
    @Deprecated
    @Override
    public void setRequestTimeout(int requestTimeoutMs) {
        setRequestTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * @deprecated use {@link #autoAuthorization(String, String, String, net.lightbody.bmp.proxy.auth.AuthType)}
     */
    @Deprecated
//    @Override
    public void autoBasicAuthorization(String domain, String username, String password) {
        autoAuthorization(domain, username, password, AuthType.BASIC);
    }

    @Override
    public void rewriteUrl(String pattern, String replace) {
        rewriteRules.add(new RewriteRule(pattern, replace));
    }

    @Override
    public void rewriteUrls(Map<String, String> rewriteRules) {
        List<RewriteRule> newRules = new ArrayList<RewriteRule>(rewriteRules.size());
        for (Map.Entry<String, String> rewriteRule : rewriteRules.entrySet()) {
            RewriteRule newRule = new RewriteRule(rewriteRule.getKey(), rewriteRule.getValue());
            newRules.add(newRule);
        }

        this.rewriteRules = new CopyOnWriteArrayList<RewriteRule>(newRules);
    }

    @Override
    public void clearRewriteRules() {
        rewriteRules.clear();
    }

    @Override
    public void blacklistRequests(String pattern, int responseCode) {
        blacklistEntries.add(new BlacklistEntry(pattern, responseCode));
    }

    @Override
    public void blacklistRequests(String pattern, int responseCode, String method) {
        blacklistEntries.add(new BlacklistEntry(pattern, responseCode, method));
    }

    @Override
    public void setBlacklist(Collection<BlacklistEntry> blacklist) {
        this.blacklistEntries = new CopyOnWriteArrayList<BlacklistEntry>(blacklist);
    }

    /**
     * @deprecated use getBlacklist()
     */
    @Deprecated
    public List<BlacklistEntry> getBlacklistedRequests() {
        return ImmutableList.copyOf(blacklistEntries);
    }

    /**
     * @deprecated use {@link #getBlacklist()}
     */
    @Override
    @Deprecated
    public Collection<BlacklistEntry> getBlacklistedUrls() {
        return getBlacklist();
    }

    @Override
    public Collection<BlacklistEntry> getBlacklist() {
        return Collections.unmodifiableCollection(blacklistEntries);
    }

    @Override
    public boolean isWhitelistEnabled() {
        return whitelist.get().isEnabled();
    }

    /**
     * @deprecated use {@link #getWhitelistUrls()}
     */
    @Deprecated
    public List<Pattern> getWhitelistRequests() {
        return ImmutableList.copyOf(whitelist.get().getPatterns());
    }

    @Override
    public Collection<String> getWhitelistUrls() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (Pattern pattern : whitelist.get().getPatterns()) {
            builder.add(pattern.pattern());
        }

        return builder.build();
    }

    /**
     * @deprecated use {@link #getWhitelistStatusCode()}
     */
    @Override
    @Deprecated
    public int getWhitelistResponseCode() {
        return getWhitelistStatusCode();
    }

    @Override
    public int getWhitelistStatusCode() {
        return whitelist.get().getStatusCode();
    }

    @Override
    public void clearBlacklist() {
        blacklistEntries.clear();
    }

    @Override
    public void whitelistRequests(Collection<String> urlPatterns, int statusCode) {
        this.whitelist.set(new Whitelist(urlPatterns, statusCode));
    }

    @Override
    public void addWhitelistPattern(String urlPattern) {
        // to make sure this method is threadsafe, we need to guarantee that the "snapshot" of the whitelist taken at the beginning
        // of the method has not been replaced by the time we have constructed a new whitelist at the end of the method
        boolean whitelistUpdated = false;
        while (!whitelistUpdated) {
            Whitelist currentWhitelist = this.whitelist.get();
            if (!currentWhitelist.isEnabled()) {
                throw new IllegalStateException("Whitelist is disabled. Cannot add patterns to a disabled whitelist.");
            }

            // retrieve the response code and list of patterns from the current whitelist, the construct a new list of patterns that contains
            // all of the old whitelist's patterns + this new pattern
            int statusCode = currentWhitelist.getStatusCode();
            List<String> newPatterns = new ArrayList<String>(currentWhitelist.getPatterns().size() + 1);
            for (Pattern pattern : currentWhitelist.getPatterns()) {
                newPatterns.add(pattern.pattern());
            }
            newPatterns.add(urlPattern);

            // create a new (immutable) Whitelist object with the new pattern list and status code
            Whitelist newWhitelist = new Whitelist(newPatterns, statusCode);

            // replace the current whitelist with the new whitelist only if the current whitelist has not changed since we started
            whitelistUpdated = this.whitelist.compareAndSet(currentWhitelist, newWhitelist);
        }
    }

    /**
     * Whitelist the specified request patterns, returning the specified responseCode for non-whitelisted
     * requests.
     *
     * @param patterns     regular expression strings matching URL patterns to whitelist. if empty or null,
     *                     the whitelist will be enabled but will not match any URLs.
     * @param responseCode the HTTP response code to return for non-whitelisted requests
     */
    public void whitelistRequests(String[] patterns, int responseCode) {
        if (patterns == null || patterns.length == 0) {
            this.enableEmptyWhitelist(responseCode);
        } else {
            this.whitelistRequests(Arrays.asList(patterns), responseCode);
        }
    }

    @Override
    public void enableEmptyWhitelist(int statusCode) {
        whitelist.set(new Whitelist(statusCode));
    }

    /**
     * @deprecated use {@link #disableWhitelist()}
     */
    @Override
    @Deprecated
    public void clearWhitelist() {
        this.disableWhitelist();
    }

    @Override
    public void disableWhitelist() {
        whitelist.set(Whitelist.WHITELIST_DISABLED);
    }

    @Override
    public void addHeader(String name, String value) {
        additionalHeaders.put(name, value);
    }

    @Override
    public void removeHeader(String name) {
        additionalHeaders.remove(name);
    }

    @Override
    public void removeAllHeaders() {
        additionalHeaders.clear();
    }

    @Override
    public Map<String, String> getAllHeaders() {
        return ImmutableMap.copyOf(additionalHeaders);
    }

    @Override
    public void setHostNameResolver(AdvancedHostResolver resolver) {
        delegatingResolver.setResolver(resolver);
    }

    @Override
    public AdvancedHostResolver getHostNameResolver() {
        return delegatingResolver.getResolver();
    }

    /**
     * @deprecated Manipulate the DNS cache directly using {@link net.lightbody.bmp.proxy.dns.AdvancedHostResolver#clearDNSCache()}.
     * See {@link #getHostNameResolver()} and the default implementation in {@link net.lightbody.bmp.proxy.dns.ChainedHostResolver}.
     */
    @Override
    @Deprecated
    public void clearDNSCache() {
        AdvancedHostResolver resolver = delegatingResolver.getResolver();
        resolver.clearDNSCache();
    }

    /**
     * @deprecated Manipulate the DNS cache directly using {@link net.lightbody.bmp.proxy.dns.AdvancedHostResolver#setNegativeDNSCacheTimeout(int, java.util.concurrent.TimeUnit)}
     * and {@link net.lightbody.bmp.proxy.dns.AdvancedHostResolver#setPositiveDNSCacheTimeout(int, java.util.concurrent.TimeUnit)}.
     * See {@link #getHostNameResolver()} and the default implementation in {@link net.lightbody.bmp.proxy.dns.ChainedHostResolver}.
     */
    @Override
    @Deprecated
    public void setDNSCacheTimeout(int timeout) {
        AdvancedHostResolver resolver = delegatingResolver.getResolver();
        resolver.setPositiveDNSCacheTimeout(timeout, TimeUnit.SECONDS);
        resolver.setNegativeDNSCacheTimeout(timeout, TimeUnit.SECONDS);
    }

    /**
     * @deprecated use {@link #waitForQuiescence(long, long, java.util.concurrent.TimeUnit)}
     */
    @Override
    @Deprecated
    public void waitForNetworkTrafficToStop(long quietPeriodInMs, long timeoutInMs) {
        waitForQuiescence(quietPeriodInMs, timeoutInMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean waitForQuiescence(long quietPeriod, long timeout, TimeUnit timeUnit) {
        return activityMonitor.waitForQuiescence(quietPeriod, timeout, timeUnit);
    }

    /**
     * Instructs this proxy to route traffic through an upstream proxy. Proxy chaining is not compatible with man-in-the-middle
     * SSL, so HAR capture will be disabled for HTTPS traffic when using an upstream proxy.
     *
     * @param chainedProxyAddress address of the upstream proxy
     */
    @Override
    public void setChainedProxy(InetSocketAddress chainedProxyAddress) {
        upstreamProxyAddress = chainedProxyAddress;
    }

    @Override
    public InetSocketAddress getChainedProxy() {
        return upstreamProxyAddress;
    }

    @Override
    public void addFirstHttpFilterFactory(HttpFiltersSource filterFactory) {
        filterFactories.add(0, filterFactory);
    }

    @Override
    public void addLastHttpFilterFactory(HttpFiltersSource filterFactory) {
        filterFactories.add(filterFactory);
    }

    /**
     * <b>Note:</b> The current implementation of this method forces a maximum response size of 2 MiB. To adjust the maximum response size, or
     * to disable aggregation (which disallows access to the {@link net.lightbody.bmp.util.HttpMessageContents}), you may add the filter source
     * directly: <code>addFirstHttpFilterFactory(new ResponseFilterAdapter.FilterSource(filter, bufferSizeInBytes));</code>
     */
    @Override
    public void addResponseFilter(ResponseFilter filter) {
        addLastHttpFilterFactory(new ResponseFilterAdapter.FilterSource(filter));
    }

    /**
     * <b>Note:</b> The current implementation of this method forces a maximum request size of 2 MiB. To adjust the maximum request size, or
     * to disable aggregation (which disallows access to the {@link net.lightbody.bmp.util.HttpMessageContents}), you may add the filter source
     * directly: <code>addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, bufferSizeInBytes));</code>
     */
    @Override
    public void addRequestFilter(RequestFilter filter) {
        addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter));
    }

    /**
     * @deprecated use {@link #setChainedProxy(java.net.InetSocketAddress)} to set an upstream proxy
     */
    @Deprecated
    public void setOptions(Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return;
        }

        String httpProxy = options.get("httpProxy");
        if (httpProxy != null) {
            log.warn("Chained proxy support through setOptions is deprecated. Use setUpstreamProxy() to enable chained proxy support.");

            HostAndPort hostAndPort = HostAndPort.fromString(httpProxy);
            this.setChainedProxy(new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPortOrDefault(80)));
        } else {
            if (errorOnUnsupportedOperation) {
                throw new UnsupportedOperationException("The LittleProxy-based implementation of BrowserMob Proxy does not support the setOptions method. Use the methods defined in the BrowserMobProxy interface to set connection parameters.");
            } else {
                log.warn("The LittleProxy-based implementation of BrowserMob Proxy does not support the setOptions method. Use the methods defined in the BrowserMobProxy interface to set connection parameters.");
            }
        }
    }

    @Override
    public Map<String, String> getRewriteRules() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (RewriteRule rewriteRule : rewriteRules) {
            builder.put(rewriteRule.getPattern().pattern(), rewriteRule.getReplace());
        }

        return builder.build();
    }

    @Override
    public void removeRewriteRule(String urlPattern) {
        // normally removing elements from the list we are iterating over would not be possible, but since this is a CopyOnWriteArrayList
        // the iterator it returns is a "snapshot" of the list that will not be affected by removal (and that does not support removal, either)
        for (RewriteRule rewriteRule : rewriteRules) {
            if (rewriteRule.getPattern().pattern().equals(urlPattern)) {
                rewriteRules.remove(rewriteRule);
            }
        }
    }

    /**
     * @deprecated use {@link #getHarCaptureTypes()} to check for relevant {@link net.lightbody.bmp.proxy.CaptureType}
     */
    @Deprecated
    public boolean isCaptureHeaders() {
        return harCaptureTypes.containsAll(CaptureType.getHeaderCaptureTypes());
    }

    /**
     * @deprecated use {@link #setHarCaptureTypes(java.util.Set)} to set the appropriate {@link net.lightbody.bmp.proxy.CaptureType}
     */
    @Deprecated
    public void setCaptureHeaders(boolean captureHeaders) {
        if (captureHeaders) {
            harCaptureTypes.addAll(CaptureType.getHeaderCaptureTypes());
        } else {
            harCaptureTypes.removeAll(CaptureType.getHeaderCaptureTypes());
        }
    }

    /**
     * @deprecated use {@link #getHarCaptureTypes()} to check for relevant {@link net.lightbody.bmp.proxy.CaptureType}
     */
    @Deprecated
    public boolean isCaptureContent() {
        return harCaptureTypes.containsAll(CaptureType.getNonBinaryContentCaptureTypes());
    }

    /**
     * @deprecated use {@link #setHarCaptureTypes(java.util.Set)} to set the appropriate {@link net.lightbody.bmp.proxy.CaptureType}
     */
    @Deprecated
    public void setCaptureContent(boolean captureContent) {
        if (captureContent) {
            harCaptureTypes.addAll(CaptureType.getAllContentCaptureTypes());
        } else {
            harCaptureTypes.removeAll(CaptureType.getAllContentCaptureTypes());
        }
    }

    /**
     * @deprecated use {@link #getHarCaptureTypes()} to check for relevant {@link net.lightbody.bmp.proxy.CaptureType}
     */
    @Deprecated
    public boolean isCaptureBinaryContent() {
        return harCaptureTypes.containsAll(CaptureType.getBinaryContentCaptureTypes());
    }

    /**
     * @deprecated use {@link #setHarCaptureTypes(java.util.Set)} to set the appropriate {@link net.lightbody.bmp.proxy.CaptureType}
     */
    @Deprecated
    public void setCaptureBinaryContent(boolean captureBinaryContent) {
        if (captureBinaryContent) {
            harCaptureTypes.addAll(CaptureType.getBinaryContentCaptureTypes());
        } else {
            harCaptureTypes.removeAll(CaptureType.getBinaryContentCaptureTypes());
        }
    }

    /**
     * When true, this implementation of BrowserMobProxy will throw an UnsupportedOperationException when a method is not supported. This
     * helps identify functionality that is not supported by the LittleProxy-based implementation. By default, this implementation will
     * log a warning rather than throw an UnsupportedOperationException.
     *
     * @param errorOnUnsupportedOperation when true, throws an exception when an operation is not supported
     */
    public void setErrorOnUnsupportedOperation(boolean errorOnUnsupportedOperation) {
        this.errorOnUnsupportedOperation = errorOnUnsupportedOperation;
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public HarPage getCurrentHarPage() {
        return currentHarPage;
    }

    public void addHttpFilterFactory(HttpFiltersSource filterFactory) {
        filterFactories.add(filterFactory);
    }

    public List<HttpFiltersSource> getFilterFactories() {
        return filterFactories;
    }

    @Override
    public void setMitmDisabled(boolean mitmDisabled) throws IllegalStateException {
        if (isStarted()) {
            throw new IllegalStateException("Cannot disable MITM after the proxy has been started");
        }

        this.mitmDisabled = mitmDisabled;
    }

    public boolean isMitmDisabled() {
        return this.mitmDisabled;
    }

    /**
     * Adds the basic browsermob-proxy filters, except for the relatively-expensive HAR capture filter.
     */
    protected void addBrowserMobFilters() {
        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new RegisterRequestFilter(originalRequest, ctx, activityMonitor);
            }
        });

        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new HttpsOriginalHostCaptureFilter(originalRequest, ctx);
            }
        });

        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new BlacklistFilter(originalRequest, ctx, getBlacklist());
            }
        });

        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                Whitelist currentWhitelist = whitelist.get();
                return new WhitelistFilter(originalRequest, ctx, isWhitelistEnabled(), currentWhitelist.getStatusCode(), currentWhitelist.getPatterns());
            }
        });

        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new RewriteUrlFilter(originalRequest, ctx, rewriteRules);
            }
        });

        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new HttpsHostCaptureFilter(originalRequest, ctx);
            }
        });

        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new AddHeadersFilter(originalRequest, additionalHeaders);
            }
        });

        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new LatencyFilter(originalRequest, latencyMs);
            }
        });

        addHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new UnregisterRequestFilter(originalRequest, ctx, activityMonitor);
            }
        });
    }

    private int getMaximumRequestBufferSize() {
        int maxBufferSize = 0;
        for (HttpFiltersSource source : filterFactories) {
            int requestBufferSize = source.getMaximumRequestBufferSizeInBytes();
            if (requestBufferSize > maxBufferSize) {
                maxBufferSize = requestBufferSize;
            }
        }

        return maxBufferSize;
    }

    private int getMaximumResponseBufferSize() {
        int maxBufferSize = 0;
        for (HttpFiltersSource source : filterFactories) {
            int requestBufferSize = source.getMaximumResponseBufferSizeInBytes();
            if (requestBufferSize > maxBufferSize) {
                maxBufferSize = requestBufferSize;
            }
        }

        return maxBufferSize;
    }

    /**
     * Enables the HAR capture filter if it has not already been enabled. The filter will be added to the end of the filter chain.
     * The HAR capture filter is relatively expensive, so this method is only called when a HAR is requested.
     */
    protected void addHarCaptureFilter() {
        if (harCaptureFilterEnabled.compareAndSet(false, true)) {
            addHttpFilterFactory(new HttpFiltersSourceAdapter() {
                @Override
                public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                    Har har = getHar();
                    if (har != null) {
                        return new HarCaptureFilter(originalRequest, ctx, har, getCurrentHarPage() == null ? null : getCurrentHarPage().getId(), getHarCaptureTypes());
                    } else {
                        return null;
                    }
                }
            });

            addHttpFilterFactory(new HttpFiltersSourceAdapter() {
                @Override
                public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                    Har har = getHar();
                    if (har != null && ProxyUtils.isCONNECT(originalRequest)) {
                        return new HttpsConnectHarCaptureFilter(originalRequest, ctx, har, getCurrentHarPage() == null ? null : getCurrentHarPage().getId());
                    } else {
                        return null;
                    }
                }
            });
        }
    }
}
