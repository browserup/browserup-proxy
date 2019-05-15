/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy.bricks;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.exception.ProxyExistsException;
import com.browserup.bup.exception.ProxyPortsExhaustedException;
import com.browserup.bup.exception.UnsupportedCharsetException;
import com.browserup.bup.filters.JavascriptRequestResponseFilter;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.auth.AuthType;
import com.browserup.bup.util.BrowserUpHttpUtil;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.sitebricks.At;
import com.google.sitebricks.client.transport.Json;
import com.google.sitebricks.client.transport.Text;
import com.google.sitebricks.headless.Reply;
import com.google.sitebricks.headless.Request;
import com.google.sitebricks.headless.Service;
import com.google.sitebricks.http.Delete;
import com.google.sitebricks.http.Get;
import com.google.sitebricks.http.Post;
import com.google.sitebricks.http.Put;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

@At("/proxy")
@Service
public class ProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyResource.class);

    private final ProxyManager proxyManager;

    @Inject
    public ProxyResource(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Get
    public Reply<?> getProxies() {
        LOG.info("GET /");
        Collection<ProxyDescriptor> proxyList = proxyManager.get().stream()
                .map(proxy -> new ProxyDescriptor(proxy.getPort()))
                .collect(toList());
        return Reply.with(new ProxyListDescriptor(proxyList)).as(Json.class);
    }

    @Post
    public Reply<?> newProxy(Request request) {
        LOG.info("POST /");
        LOG.info(request.params().toString());
        String systemProxyHost = System.getProperty("http.proxyHost");
        String systemProxyPort = System.getProperty("http.proxyPort");
        String httpProxy = request.param("httpProxy");
        String proxyUsername = request.param("proxyUsername");
        String proxyPassword = request.param("proxyPassword");

        Hashtable<String, String> options = new Hashtable<String, String>();

        // If the upstream proxy is specified via query params that should override any default system level proxy.
        String upstreamHttpProxy = null;
        if (httpProxy != null) {
            upstreamHttpProxy = httpProxy;
        } else if ((systemProxyHost != null) && (systemProxyPort != null)) {
            upstreamHttpProxy  = String.format("%s:%s", systemProxyHost, systemProxyPort);
        }

        String paramBindAddr = request.param("bindAddress");
        String paramServerBindAddr = request.param("serverBindAddress");
        Integer paramPort = request.param("port") == null ? null : Integer.parseInt(request.param("port"));

        String useEccString = request.param("useEcc");
        boolean useEcc = Boolean.parseBoolean(useEccString);

        String trustAllServersString = request.param("trustAllServers");
        boolean trustAllServers = Boolean.parseBoolean(trustAllServersString);

        LOG.debug("POST proxy instance on bindAddress `{}` & port `{}` & serverBindAddress `{}`",
                paramBindAddr, paramPort, paramServerBindAddr);
        BrowserUpProxyServer proxy;
        try {
            proxy = proxyManager.create(upstreamHttpProxy, proxyUsername, proxyPassword, paramPort, paramBindAddr, paramServerBindAddr, useEcc, trustAllServers);
        } catch (ProxyExistsException ex) {
            return Reply.with(new ProxyDescriptor(ex.getPort())).status(455).as(Json.class);
        } catch (ProxyPortsExhaustedException ex) {
            return Reply.saying().status(456);
        } catch (Exception ex) {
            StringWriter s = new StringWriter();
            ex.printStackTrace(new PrintWriter(s));
            return Reply.with(s).as(Text.class).status(550);
        }
        return Reply.with(new ProxyDescriptor(proxy.getPort())).as(Json.class);
    }

    @Get
    @At("/:port/har")
    public Reply<?> getHar(@Named("port") int port) {
        LOG.info("GET /" + port + "/har");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Har har = proxy.getHar();

        return Reply.with(har).as(Json.class);
    }

    @Get
    @At("/:port/har/mostRecentEntry")
    public Reply<?> findMostRecentEntry(@Named("port") int port, Request request) {
        LOG.info("GET /" + port + "/har/entry");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Pattern pattern;
        try {
            pattern = getUrlPatternFromRequest(request);
        } catch (IllegalArgumentException ex) {
            return Reply.with(ex.getMessage()).badRequest();
        }
        return proxy.findMostRecentEntry(pattern)
                .map(entry -> (Reply) Reply.with(entry).as(Json.class))
                .orElse(Reply.with(new HarEntry()).as(Json.class));
    }

    @Get
    @At("/:port/har/mostRecentEntry/assertResponseTimeWithin")
    public Reply<?> mostRecentEntryAssertResponseTimeWithin(@Named("port") int port, Request request) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseTimeWithin");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Pattern pattern;
        try {
            pattern = getUrlPatternFromRequest(request);
        } catch (IllegalArgumentException ex) {
            return Reply.with(ex.getMessage()).badRequest();
        }

        Optional<Long> time = getAssertionTimeFromRequest(request);
        if (!time.isPresent()) {
            return Reply.with("Invalid 'milliseconds' url parameter").badRequest();
        }

        AssertionResult result = proxy.assertMostRecentResponseTimeLessThanOrEqual(pattern, time.get());
        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/:port/har/entries")
    public Reply<?> findEntries(@Named("port") int port, Request request) {
        LOG.info("GET /" + port + "/har/entries");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Pattern pattern;
        try {
            pattern = getUrlPatternFromRequest(request);
        } catch (IllegalArgumentException ex) {
            return Reply.with(ex.getMessage()).badRequest();
        }

        return Reply.with(proxy.findEntries(pattern)).as(Json.class);
    }

    @Put
    @At("/:port/har")
    public Reply<?> newHar(@Named("port") int port, Request request) {
        LOG.info("PUT /" + port + "/har");
        LOG.info(request.params().toString());
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String initialPageRef = request.param("initialPageRef");
        String initialPageTitle = request.param("initialPageTitle");
        Har oldHar = proxy.newHar(initialPageRef, initialPageTitle);

        String captureHeaders = request.param("captureHeaders");
        String captureContent = request.param("captureContent");
        String captureBinaryContent = request.param("captureBinaryContent");
        Set<CaptureType> captureTypes = new HashSet<CaptureType>();
        if (Boolean.parseBoolean(captureHeaders)) {
            captureTypes.addAll(CaptureType.getHeaderCaptureTypes());
        }
        if (Boolean.parseBoolean(captureContent)) {
            captureTypes.addAll(CaptureType.getAllContentCaptureTypes());
        }
        if (Boolean.parseBoolean(captureBinaryContent)) {
            captureTypes.addAll(CaptureType.getBinaryContentCaptureTypes());
        }
        proxy.setHarCaptureTypes(captureTypes);

        String captureCookies = request.param("captureCookies");
        if (proxy instanceof BrowserUpProxyServer && Boolean.parseBoolean(captureCookies)) {
            BrowserUpProxyServer BrowserUpProxyServer = (BrowserUpProxyServer) proxy;
            BrowserUpProxyServer.enableHarCaptureTypes(CaptureType.getCookieCaptureTypes());
        }

        if (oldHar != null) {
            return Reply.with(oldHar).as(Json.class);
        } else {
            return Reply.saying().noContent();
        }
    }

    @Put
    @At("/:port/har/pageRef")
    public Reply<?> setPage(@Named("port") int port, Request request) {
        LOG.info("PUT /" + port + "/har/pageRef");
        LOG.info(request.params().toString());
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String pageRef = request.param("pageRef");
        String pageTitle = request.param("pageTitle");
        proxy.newPage(pageRef, pageTitle);

        return Reply.saying().ok();
    }

    @Post
    @At("/:port/har/commands/endPage")
    public Reply<?> endPage(@Named("port") int port, Request request) {
        LOG.info("POST /" + port + "/commands/endPage");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.endPage();

        return Reply.saying().ok();
    }

    @Post
    @At("/:port/har/commands/endHar")
    public Reply<?> endHar(@Named("port") int port, Request request) {
        LOG.info("POST /" + port + "/commands/endHar");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.endHar();

        return Reply.saying().ok();
    }

    @Get
    @At("/:port/blacklist")
    public Reply<?> getBlacklist(@Named("port") int port, Request request) {
        LOG.info("GET /" + port + "/blacklist");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        return Reply.with(proxy.getBlacklist()).as(Json.class);
    }

    @Put
    @At("/:port/blacklist")
    public Reply<?> blacklist(@Named("port") int port, Request request) {
        LOG.info("PUT /" + port + "/blacklist");
        LOG.info(request.params().toString());
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String blacklist = request.param("regex");
        int responseCode = parseResponseCode(request.param("status"));
        String method = request.param("method");
        proxy.blacklistRequests(blacklist, responseCode, method);

        return Reply.saying().ok();
    }

    @Delete
    @At("/:port/blacklist")
    public Reply<?> clearBlacklist(@Named("port") int port, Request request) {
        LOG.info("DELETE /" + port + "/blacklist");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.clearBlacklist();
        return Reply.saying().ok();
    }

    @Get
    @At("/:port/whitelist")
    public Reply<?> getWhitelist(@Named("port") int port, Request request) {
        LOG.info("GET /" + port + "/whitelist");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        return Reply.with(proxy.getWhitelistUrls()).as(Json.class);
    }

    @Put
    @At("/:port/whitelist")
    public Reply<?> whitelist(@Named("port") int port, Request request) {
        LOG.info("PUT /" + port + "/whitelist");
        LOG.info(request.params().toString());
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String regex = request.param("regex");
        int responseCode = parseResponseCode(request.param("status"));
        proxy.whitelistRequests(regex.split(","), responseCode);

        return Reply.saying().ok();
    }

    @Delete
    @At("/:port/whitelist")
    public Reply<?> clearWhitelist(@Named("port") int port, Request request) {
        LOG.info("DELETE /" + port + "/whitelist");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.disableWhitelist();
        return Reply.saying().ok();
    }

    @Post
    @At("/:port/auth/basic/:domain")
    public Reply<?> autoBasicAuth(@Named("port") int port, @Named("domain") String domain, Request request) {
        LOG.info("POST /" + port + "/auth/basic/" + domain);
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Map<String, String> credentials = request.read(HashMap.class).as(Json.class);
        proxy.autoAuthorization(domain, credentials.get("username"), credentials.get("password"), AuthType.BASIC);

        return Reply.saying().ok();
    }

    @Post
    @At("/:port/headers")
    public Reply<?> updateHeaders(@Named("port") int port, Request request) {
        LOG.info("POST /" + port + "/headers");

        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Map<String, String> headers = request.read(Map.class).as(Json.class);
        headers.forEach(proxy::addHeader);
        return Reply.saying().ok();
    }

    @Post
    @At("/:port/filter/request")
    public Reply<?> addRequestFilter(@Named("port") int port, Request request) throws IOException, ScriptException {
        LOG.info("POST /" + port + "/filter/request");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        JavascriptRequestResponseFilter requestResponseFilter = new JavascriptRequestResponseFilter();

        String script = getEntityBodyFromRequest(request);
        requestResponseFilter.setRequestFilterScript(script);

        proxy.addRequestFilter(requestResponseFilter);

        return Reply.saying().ok();
    }

    @Post
    @At("/:port/filter/response")
    public Reply<?> addResponseFilter(@Named("port") int port, Request request) throws IOException, ScriptException {
        LOG.info("POST /" + port + "/filter/response");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        JavascriptRequestResponseFilter requestResponseFilter = new JavascriptRequestResponseFilter();

        String script = getEntityBodyFromRequest(request);
        requestResponseFilter.setResponseFilterScript(script);

        proxy.addResponseFilter(requestResponseFilter);

        return Reply.saying().ok();
    }

    @Put
    @At("/:port/limit")
    public Reply<?> limit(@Named("port") int port, Request request) {
        LOG.info("PUT /" + port + "/limit");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String upstreamKbps = request.param("upstreamKbps");
        if (upstreamKbps != null) {
            try {
                long upstreamBytesPerSecond = Integer.parseInt(upstreamKbps) * 1024;
                proxy.setWriteBandwidthLimit(upstreamBytesPerSecond);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid upstreamKbps value");
                return Reply.saying().badRequest();
            }
        }

        String upstreamBps = request.param("upstreamBps");
        if (upstreamBps != null) {
            try {
                proxy.setWriteBandwidthLimit(Integer.parseInt(upstreamBps));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid upstreamBps value");
                return Reply.saying().badRequest();
            }
        }

        String downstreamKbps = request.param("downstreamKbps");
        if (downstreamKbps != null) {
            try {
                long downstreamBytesPerSecond = Integer.parseInt(downstreamKbps) * 1024;
                proxy.setReadBandwidthLimit(downstreamBytesPerSecond);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid downstreamKbps value");
                return Reply.saying().badRequest();
            }
        }

        String downstreamBps = request.param("downstreamBps");
        if (downstreamBps != null) {
            try {
                proxy.setReadBandwidthLimit(Integer.parseInt(downstreamBps));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid downstreamBps value");
                return Reply.saying().badRequest();
            }
        }

        String latency = request.param("latency");
        if (latency != null) {
            try {
                proxy.setLatency(Long.parseLong(latency), TimeUnit.MILLISECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid latency value");
                return Reply.saying().badRequest();
            }
        }

        if (request.param("upstreamMaxKB") != null) {
            LOG.warn("upstreamMaxKB no longer supported");
            return Reply.saying().badRequest();
        }
        if (request.param("downstreamMaxKB") != null) {
            LOG.warn("downstreamMaxKB no longer supported");
            return Reply.saying().badRequest();
        }
        if (request.param("payloadPercentage") != null) {
            LOG.warn("payloadPercentage no longer supported");
            return Reply.saying().badRequest();
        }
        if (request.param("maxBitsPerSecond") != null) {
            LOG.warn("maxBitsPerSecond no longer supported");
            return Reply.saying().badRequest();
        }
        if (request.param("enable") != null) {
            LOG.warn("enable no longer supported. Limits, if set, will always be enabled.");
            return Reply.saying().badRequest();
        }

        return Reply.saying().ok();
    }

    @Put
    @At("/:port/timeout")
    public Reply<?> timeout(@Named("port") int port, Request request) {
        LOG.info("PUT /" + port + "/timeout");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String requestTimeout = request.param("requestTimeout");
        if (requestTimeout != null) {
            try {
                proxy.setRequestTimeout(Integer.parseInt(requestTimeout), TimeUnit.MILLISECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid requestTimeout value");
                return Reply.saying().badRequest();
            }
        }
        String readTimeout = request.param("readTimeout");
        if (readTimeout != null) {
            try {
                proxy.setIdleConnectionTimeout(Integer.parseInt(readTimeout), TimeUnit.MILLISECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid readTimeout value");
                return Reply.saying().badRequest();
            }
        }
        String connectionTimeout = request.param("connectionTimeout");
        if (connectionTimeout != null) {
            try {
                proxy.setConnectTimeout(Integer.parseInt(connectionTimeout), TimeUnit.MILLISECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid connectionTimeout value");
                return Reply.saying().badRequest();
            }
        }
        String dnsCacheTimeout = request.param("dnsCacheTimeout");
        if (dnsCacheTimeout != null) {
            try {
                proxy.getHostNameResolver().setPositiveDNSCacheTimeout(Integer.parseInt(dnsCacheTimeout), TimeUnit.SECONDS);
                proxy.getHostNameResolver().setNegativeDNSCacheTimeout(Integer.parseInt(dnsCacheTimeout), TimeUnit.SECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid dnsCacheTimeout value");
                return Reply.saying().badRequest();
            }
        }
        return Reply.saying().ok();
    }

    @Delete
    @At("/:port")
    public Reply<?> delete(@Named("port") int port) {
        LOG.info("DELETE /" + port);
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxyManager.delete(port);
        return Reply.saying().ok();
    }

    @Post
    @At("/:port/hosts")
    public Reply<?> remapHosts(@Named("port") int port, Request request) {
        LOG.info("POST /" + port + "/hosts");
        LOG.info(request.params().toString());
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        @SuppressWarnings("unchecked") Map<String, String> headers = request.read(Map.class).as(Json.class);

        headers.forEach((key, value) -> {
            proxy.getHostNameResolver().remapHost(key, value);
            proxy.getHostNameResolver().setNegativeDNSCacheTimeout(0, TimeUnit.SECONDS);
            proxy.getHostNameResolver().setPositiveDNSCacheTimeout(0, TimeUnit.SECONDS);
            proxy.getHostNameResolver().clearDNSCache();
        });

        return Reply.saying().ok();
    }


    @Put
    @At("/:port/wait")
    public Reply<?> wait(@Named("port") int port, Request request) {
        LOG.info("PUT /" + port + "/wait");
        LOG.info(request.params().toString());
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String quietPeriodInMs = request.param("quietPeriodInMs");
        String timeoutInMs = request.param("timeoutInMs");
        proxy.waitForQuiescence(Long.parseLong(quietPeriodInMs), Long.parseLong(timeoutInMs), TimeUnit.MILLISECONDS);
        return Reply.saying().ok();
    }

    @Delete
    @At("/:port/dns/cache")
    public Reply<?> clearDnsCache(@Named("port") int port) {
        LOG.info("DELETE /" + port + "/dns/cache");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }
        proxy.getHostNameResolver().clearDNSCache();

        return Reply.saying().ok();
    }

    @Put
    @At("/:port/rewrite")
    public Reply<?> rewriteUrl(@Named("port") int port, Request request) {
        LOG.info("PUT /" + port + "/rewrite");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String match = request.param("matchRegex");
        String replace = request.param("replace");
        proxy.rewriteUrl(match, replace);
        return Reply.saying().ok();
    }

    @Delete
    @At("/:port/rewrite")
    public Reply<?> clearRewriteRules(@Named("port") int port, Request request) {
        LOG.info("DELETE /" + port + "/rewrite");
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.clearRewriteRules();
        return Reply.saying().ok();
    }

    @Put
    @At("/:port/retry")
    public Reply<?> retryCount(@Named("port") int port, Request request) {
        LOG.warn("/port/retry API is no longer supported");
        return Reply.saying().badRequest();
    }

    private int parseResponseCode(String response) {
        int responseCode = 200;
        if (response != null) {
            try {
                responseCode = Integer.parseInt(response);
            } catch (NumberFormatException e) {
            }
        }
        return responseCode;
    }

    public static class ProxyDescriptor {
        private int port;

        public ProxyDescriptor() {
        }

        public ProxyDescriptor(int port) {
            this.port = port;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class ProxyListDescriptor {
        private Collection<ProxyDescriptor> proxyList;

        public ProxyListDescriptor() {
        }

        public ProxyListDescriptor(Collection<ProxyDescriptor> proxyList) {
            this.proxyList = proxyList;
        }

        public Collection<ProxyDescriptor> getProxyList() {
            return proxyList;
        }

        public void setProxyList(Collection<ProxyDescriptor> proxyList) {
            this.proxyList = proxyList;
        }
    }

    private String getEntityBodyFromRequest(Request request) throws IOException {
        String contentTypeHeader = request.header("Content-Type");
        Charset charset = null;
        try {
            charset = BrowserUpHttpUtil.readCharsetInContentTypeHeader(contentTypeHeader);
        } catch (UnsupportedCharsetException e) {
            java.nio.charset.UnsupportedCharsetException cause = e.getUnsupportedCharsetExceptionCause();
            LOG.error("Character set declared in Content-Type header is not supported. Content-Type header: {}", contentTypeHeader, cause);

            throw cause;
        }

        if (charset == null) {
            charset = BrowserUpHttpUtil.DEFAULT_HTTP_CHARSET;
        }

        ByteArrayOutputStream entityBodyBytes = new ByteArrayOutputStream();
        request.readTo(entityBodyBytes);

        return new String(entityBodyBytes.toByteArray(), charset);
    }

    private Pattern getUrlPatternFromRequest(Request request) throws IllegalArgumentException {
        String urlParam = request.param("urlPattern");
        if (StringUtils.isEmpty(urlParam)) {
            LOG.warn("Url parameter not present");
            throw new IllegalArgumentException("URL parameter 'urlPattern' is mandatory");
        }

        Pattern urlPattern;
        try {
            urlPattern = Pattern.compile(urlParam);
        } catch (Exception ex) {
            LOG.warn("Url parameter not valid", ex);
            throw new IllegalArgumentException("URL parameter 'urlPattern' is not a valid regexp");
        }
        return urlPattern;
    }

    private Optional<Long> getAssertionTimeFromRequest(Request request) {
        String timeParam = request.param("milliseconds");
        if (StringUtils.isEmpty(timeParam)) {
            LOG.warn("Time parameter not present");
            return Optional.empty();
        }

        Long time;
        try {
            time = Long.valueOf(timeParam);
        } catch (Exception ex) {
            LOG.warn("Time parameter not valid", ex);
            return Optional.empty();
        }
        return Optional.of(time);
    }
}
