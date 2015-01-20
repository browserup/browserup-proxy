package net.lightbody.bmp.proxy;

import net.lightbody.bmp.core.har.Har;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public interface BrowserMobProxyServer {
    // configuration / pre-start() methods
    int getPort();
    void setPort(int port);

    // proxy server control methods
    void start();

    /**
     * Stops accepting new client connections and initiates a graceful shutdown of the proxy server, waiting for network traffic to stop.
     * TODO: define a time limit to wait for network traffic to stop
     */
    void stop();

    //new method
    /**
     * Like {@link #stop()}, shuts down the proxy server and no longer accepts incoming connections, but does not wait for any existing
     * network traffic to cease. Any existing connections to clients or to servers may be force-killed immediately.
     */
    void abort();

    // removed throws NameResolutionException, which is Jetty impl-specific
    org.openqa.selenium.Proxy seleniumProxy(); //throws NameResolutionException;

    // Jetty impl-specific
    //void cleanup();

    // renamed because it wasn't descriptive of what it does: returns the address of the interface on which the proxy is listening for client connections
    //InetAddress getLocalHost();
    InetSocketAddress getClientConnectionBindAddress();

    // renamed. what this actually does is sets the address of the interface to bind to.
    //void setLocalHost(InetAddress localHost);
    void setClientConnectionBindAddress(InetSocketAddress bindAddress);

    // new method: analogous to above for communications to the server. this is the address of the NIC on this machine that will initiate connections to the server.
    InetSocketAddress getServerConnectionBindAddress();
    void setServerConnectionBindAddress(InetSocketAddress bindAddress);

    //TODO: move this to a utility class
    //InetAddress getConnectableLocalHost() throws UnknownHostException;

    // HAR capture features
    Har getHar();
    Har newHar(String initialPageRef);
    void newPage(String pageRef);
    void endPage();
    //new method
    /**
     * Stops capturing traffic in the HAR.
     * @return the existing HAR
     */
    Har endHar();
    void setCaptureHeaders(boolean captureHeaders);
    void setCaptureContent(boolean captureContent);
    void setCaptureBinaryContent(boolean captureBinaryContent);
    //new method
    void setCaptureCookies(boolean captureCookies);

    // interceptors are necessarily specific to the implementation. there's not a generic way to implement this functionality.
    // essentially, this is a breaking change when moving to LP.
//    @Deprecated
//    void addRequestInterceptor(HttpRequestInterceptor i);
//    void addRequestInterceptor(RequestInterceptor interceptor);
//    @Deprecated
//    void addResponseInterceptor(HttpResponseInterceptor i);
//    void addResponseInterceptor(ResponseInterceptor interceptor);

    // StreamManager is Jetty impl-specific. replace with methods below.
//    StreamManager getStreamManager();

    // renamed these methods to be more explicit
//    @Deprecated
//    void setDownstreamKbps(long downstreamKbps);
//    @Deprecated
//    void setUpstreamKbps(long upstreamKbps);
    void setReadLimitKbps(long readLimitKbps);
    void setWriteLimitKbps(long writeLimitKbps);

    // replace deprecated setLatency with a method that takes an explicit TimeUnit
    //void setLatency(long latency)
    void setLatency(long latency, TimeUnit timeUnit);

    // network settings
//    void setRequestTimeout(int requestTimeout);
    void setRequestTimeout(int requestTimeout, TimeUnit timeUnit);
//    void setSocketOperationTimeout(int readTimeout);
    void setSocketOperationTimeout(int readTimeout, TimeUnit timeUnit);
//    void setConnectionTimeout(int connectionTimeout);
    void setConnectionTimeout(int connectionTimeout, TimeUnit timeUnit);

    void autoBasicAuthorization(String domain, String username, String password);

    void rewriteUrl(String match, String replace);
    void clearRewriteRules();
    //new method
    //TODO: thinking about making this signature return a Map<Pattern, String> instead of a Collection of RewriteRules that encapsulates Map<Pattern, String>
    /**
     * @return all RewriteRules currently in effect.
     */
    Collection<RewriteRule> getRewriteRules();

    void blacklistRequests(String pattern, int responseCode);
    void blacklistRequests(String pattern, int responseCode, String method);
    Collection<BlacklistEntry> getBlacklistedUrls();
    void clearBlacklist();

//    @Deprecated
//    List<BlacklistEntry> getBlacklistedRequests();
//    @Deprecated
//    List<Pattern> getWhitelistRequests();

    void whitelistRequests(String[] patterns, int responseCode);
    void enableEmptyWhitelist(int responseCode);
    void clearWhitelist();
    Collection<Pattern> getWhitelistUrls();
    int getWhitelistResponseCode();
    boolean isWhitelistEnabled();

    void setRetryCount(int count);

    void addHeader(String name, String value);
    //new method

    /**
     * Removes a header previously added with the {@link #addHeader(String name, String value)} method.
     * @param name previously-added header's name
     */
    void removeHeader(String name);

    // DNS manipulation
    void clearDNSCache();
//    void setDNSCacheTimeout(int timeout);
    void setDNSCacheTimeout(int timeout, TimeUnit timeUnit);
    void remapHost(String source, String target);
    //new method
    Map<String, String> getHostRemappings();

    // modified this method's return value
    /**
     * Waits for existing network traffic to stop, and for the specified quietPeriod to elapse. Returns true if there is no network traffic
     * for the quiet period within the specified timeout, otherwise returns false.
     * @param quietPeriod amount of time after which network traffic will be considered "stopped"
     * @param timeout maximum amount of time to wait for network traffic to stop
     * @param timeUnit TimeUnit for the quietPeriod and timeout
     * @return true if network traffic is stopped, otherwise false
     */
    boolean waitForNetworkTrafficToStop(long quietPeriod, long timeout, TimeUnit timeUnit);

    //new methods: support for upstream chained proxy.
    void setChainedProxyAddress(InetSocketAddress chainedProxyAddress);
    InetSocketAddress getChainedProxyAddress();

    // Jetty impl-specific. chained proxy support in setChainedProxy
//    void setOptions(Map<String, String> options);
}
