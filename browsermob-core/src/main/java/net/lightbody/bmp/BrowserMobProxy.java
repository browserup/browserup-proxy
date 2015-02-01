package net.lightbody.bmp.proxy;

import java.net.InetAddress;
import net.lightbody.bmp.core.har.Har;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.lightbody.bmp.proxy.http.BrowserMobHttpClient;

public interface BrowserMobProxyServer {
    // configuration / pre-start() methods
    int getPort();    
    InetAddress getClientBindAddress();
    InetAddress getServerBindAddress();
    
    // proxy server control methods    
    // start proxy on port, bind to 0.0.0.0
    void start(int port);    
    //bind both client and server to bindAddress
    void start(int port, InetAddress bindAddress);    
    
    void start(int port, InetAddress clientBindAddress, InetAddress serverBindAddress);

    /**
     * Stops accepting new client connections and initiates a graceful shutdown of the proxy server, waiting for network traffic to stop.
     * TODO: define a time limit to wait for network traffic to stop     
     * do we need this if we already have waitForNetworkTrafficToStop() ?
     * 
     */
    void stop();
    
    //new method
    /**
     * Like {@link #stop()}, shuts down the proxy server and no longer accepts incoming connections, but does not wait for any existing
     * network traffic to cease. Any existing connections to clients or to servers may be force-killed immediately.
     */
    void abort();

    // removed throws NameResolutionException, which is Jetty impl-specific
    // I don't think we should introduce a dependency on Selenium just to provide a convenience method.
    //org.openqa.selenium.Proxy seleniumProxy(); //throws NameResolutionException;   
    
    // Jetty impl-specific
    //void cleanup();

    // bind address methods replaced with updated BrowserMobProxyServer#start()
    
    //TODO: move this to a utility class
    //InetAddress getConnectableLocalHost() throws UnknownHostException;

    // HAR capture features
    enum HarCaptureSetting{
        HEADERS, CONTENT, BINARY_CONTENT, COOKIES
    }
    
    Har getHar();
    // use a default page name
    Har newHar();    
    Har newHar(String initialPageRef);    
    Har newHar(EnumSet<HarCaptureSetting> harCaptureSettings);    
    Har newHar(String initialPageRef, EnumSet<HarCaptureSetting> harCaptureSettings);        
    void newPage(String pageRef);
    void endPage();
    //new method
    /**
     * Stops capturing traffic in the HAR.
     * @return the existing HAR
     */
    Har endHar();                
    
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
    // make the units bytes
    void setReadBandwidthLimit(long bps);
    void setWriteBandwidthLimit(long bps);
    
    void setReadDataLimit(long bytes);
    void setWriteDataLimit(long bytes);

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

    // basic by default
    void autoAuthorization(String domain, String username, String password); 
    
    void autoAuthorization(String domain, String username, String password, AuthType authType);    
    
    void rewriteUrl(String pattern, String replace);
    void clearRewriteRules();
    //new method
    //TODO: thinking about making this signature return a Map<Pattern, String> instead of a Collection of RewriteRules that encapsulates Map<Pattern, String>    
    //or return Map where key is Pattern#toString(), to make it consistent with signature of rewriteUrl
    /**
     * @return all RewriteRules currently in effect.
     */
    Map<String, String> getRewriteRules();

    void blacklistRequests(String pattern, int responseCode);
    void blacklistRequests(String pattern, int responseCode, String method);
    Collection<BlacklistEntry> getBlacklistedUrls();
    void clearBlacklist();

//    @Deprecated
//    List<BlacklistEntry> getBlacklistedRequests();
//    @Deprecated
//    List<Pattern> getWhitelistRequests();

    void whitelistRequests(int responseCode);
    void whitelistRequests(String pattern, int responseCode);
    void clearWhitelist();
    Collection<WhitelistEntry> getWhitelistUrls();    
    // TODO: make method names and signatures more consistent between blacklist / whitelist / rewrite rules ?
    
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
    boolean waitForQuiescence(long quietPeriod, long timeout, TimeUnit timeUnit);

    //new methods: support for upstream chained proxy.
    void setChainedProxyAddress(InetSocketAddress chainedProxyAddress);
    InetSocketAddress getChainedProxyAddress();

    // Jetty impl-specific. chained proxy support in setChainedProxy
//    void setOptions(Map<String, String> options);        
}
