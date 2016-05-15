# New BrowserMobProxy interface
The `BrowserMobProxyServer` class, powered by LitleProxy, implements the ``BrowserMobProxy` interface. The following table lists the current level of support for the new interface in the modern and legacy BMP implementations:

`BrowserMobProxy` method | Legacy `ProxyServer` (Jetty 5) | `BrowserMobProxyServer` (LittleProxy)
:----------------------- | :---------------------: | :-----------------------------------:
`start` (and related) | X | X
`stop` | X | X
`isStarted` | X | X
`abort` | X | X
`getClientBindAddress` | X | X
`getPort` | X | X
`getServerBindAddress` | [Will not support](#server-bind-address) | X
`getHar` | X | X
`newHar` | X | X
`setHarCaptureTypes` | [Partial support](#har-capture-types) | X
`getHarCaptureTypes` | X | X
`enableHarCaptureTypes` | X | X
`disableHarCaptureTypes` | X | X
`newPage` | X | X
`endHar` | TBD | X
`setReadBandwidthLimit` | X | X
`setWriteBandwidthLimit` | X | X
`setLatency` | X | X
`setConnectTimeout` | X | [Must be enabled before start()](#timeouts)
`setIdleConnectionTimeout` | X | [Must be enabled before start()](#timeouts)
`setRequestTimeout` | X | Planned
`autoAuthorization` | X | X
`stopAutoAuthorization` | [Will not support](#auto-authorization) | X
`rewriteUrl` | X | X
`rewriteUrls` | X | X
`removeRewriteRule` | X | X
`clearRewriteRules` | X | X
`blacklistRequests` | X | X
`setBlacklist` | X | X
`getBlacklist` | X | X
`clearBlacklist` | X | X
`whitelistRequests` | X | X
`addWhitelistPattern` | X | X
`enableEmptyWhitelist` | X | X
`disableWhitelist` | X | X
`getWhitelistUrls` | X | X
`getWhitelistStatusCode` | X | X
`isWhitelistEnabled` | X | X
`addHeaders` | X | X
`addHeader` | X | X
`removeHeader` | X | X
`removeAllHeaders` | X | X
`getAllHeaders` | X | X
`setHostNameResolver` | [Supported (see notes)](#dns-resolvers) | X
`getHostNameResolver` | [Supported (see notes)](#dns-resolvers) | X
`waitForQuiescence`  | X | X
`setChainedProxy`  | X | X
`getChainedProxy`  | X | X
`addFirstHttpFilterFactory`  | [Will not support](#interceptors) | X
`addLastHttpFilterFactory`  | [Will not support](#interceptors) | X
`addResponseFilter` | [Will not support](#interceptors) | X
`addRequestFilter` | [Will not support](#interceptors) | X

# Limitations
## Interceptors
Interceptors are tightly coupled to the underlying BrowserMob Proxy implementation (Jetty 5 or LittleProxy). As a result,
the Jetty 5-based `ProxyServer` implementation will continue to support the legacy interceptor methods, `addRequestInterceptor`
and `addResponseInterceptor`, but **will not support the new interceptor methods in `BrowserMobProxy`**. The new LittleProxy-based
implementation will fully support the new interceptor methods (`addResponseFilter`, `addRequestFilter`, `addFirstHttpFilterFactory` 
and `addLastHttpFilterFactory`), and will not support the legacy interceptor methods.

To continue using interceptors with the Jetty 5-based implementation, downcast to `LegacyProxyServer` when adding the interceptor:
```java
        BrowserMobProxy legacyImpl = new ProxyServer();
        ((LegacyProxyServer)legacyImpl).addRequestInterceptor(new RequestInterceptor() {
            @Override
            public void process(BrowserMobHttpRequest request, Har har) {
                // interceptor code goes here
            }
        });
```

## DNS resolvers
Both the legacy and new LittleProxy-based implementations support the new get/setHostNameResolver methods. The legacy implementation uses XBill/dnsjava by default, with failover to native JVM name resolution enabled by default. The LittleProxy implementation uses native name resolution by default, but fully supports the DnsJavaResolver when calling the setHostNameResolver method.

## Server bind address
The legacy implementation does not support server bind addresses. LittleProxy fully supports server bind addresses.

## HAR capture types
The legacy implementation supports all HAR capture types, but does not support controlling request and response capture types separately
(e.g. enabling content capture only for requests). Additionally, the Jetty 5 implementation does not allow disabling cookie capture.

## Timeouts
The new LittleProxy implementation requires that all timeouts be set before calling a `start()` method.

## Auto authorization
The legacy implementation does not support the `stopAutoAuthorization` method.