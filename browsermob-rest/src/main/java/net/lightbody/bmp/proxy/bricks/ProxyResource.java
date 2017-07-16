package net.lightbody.bmp.proxy.bricks;

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
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.exception.ProxyExistsException;
import net.lightbody.bmp.exception.ProxyPortsExhaustedException;
import net.lightbody.bmp.exception.UnsupportedCharsetException;
import net.lightbody.bmp.filters.JavascriptRequestResponseFilter;
import net.lightbody.bmp.proxy.BrowserMobProxyServerLegacyAdapter;
import net.lightbody.bmp.proxy.CaptureType;
import net.lightbody.bmp.proxy.ProxyManager;
import net.lightbody.bmp.util.BrowserMobHttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

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
        Collection<ProxyDescriptor> proxyList = new ArrayList<ProxyDescriptor>();
        for (BrowserMobProxyServerLegacyAdapter proxy : proxyManager.get()) {
            proxyList.add(new ProxyDescriptor(proxy.getPort()));
        }
        return Reply.with(new ProxyListDescriptor(proxyList)).as(Json.class);
    }

    @Post
    public Reply<?> newProxy(Request<String> request) {
        String systemProxyHost = System.getProperty("http.proxyHost");
        String systemProxyPort = System.getProperty("http.proxyPort");
        String httpProxy = request.param("httpProxy");
        String proxyUsername = request.param("proxyUsername");
        String proxyPassword = request.param("proxyPassword");

        Hashtable<String, String> options = new Hashtable<String, String>();

        // If the upstream proxy is specified via query params that should override any default system level proxy.
        if (httpProxy != null) {
            options.put("httpProxy", httpProxy);
        } else if ((systemProxyHost != null) && (systemProxyPort != null)) {
            options.put("httpProxy", String.format("%s:%s", systemProxyHost, systemProxyPort));
        }

        // this is a short-term work-around for Proxy Auth in the REST API until the upcoming REST API refactor
        if (proxyUsername != null && proxyPassword != null) {
            options.put("proxyUsername", proxyUsername);
            options.put("proxyPassword", proxyPassword);
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
        BrowserMobProxyServerLegacyAdapter proxy;
        try {
            proxy = proxyManager.create(options, paramPort, paramBindAddr, paramServerBindAddr, useEcc, trustAllServers);
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
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Har har = proxy.getHar();

        return Reply.with(har).as(Json.class);
    }

    @Put
    @At("/:port/har")
    public Reply<?> newHar(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String initialPageRef = request.param("initialPageRef");
        String initialPageTitle = request.param("initialPageTitle");
        Har oldHar = proxy.newHar(initialPageRef, initialPageTitle);

        String captureHeaders = request.param("captureHeaders");
        String captureContent = request.param("captureContent");
        String captureBinaryContent = request.param("captureBinaryContent");
        proxy.setCaptureHeaders(Boolean.parseBoolean(captureHeaders));
        proxy.setCaptureContent(Boolean.parseBoolean(captureContent));
        proxy.setCaptureBinaryContent(Boolean.parseBoolean(captureBinaryContent));

        String captureCookies = request.param("captureCookies");
        if (proxy instanceof BrowserMobProxyServer && Boolean.parseBoolean(captureCookies)) {
            BrowserMobProxyServer browserMobProxyServer = (BrowserMobProxyServer) proxy;
            browserMobProxyServer.enableHarCaptureTypes(CaptureType.getCookieCaptureTypes());
        }

        if (oldHar != null) {
            return Reply.with(oldHar).as(Json.class);
        } else {
            return Reply.saying().noContent();
        }
    }

    @Put
    @At("/:port/har/pageRef")
    public Reply<?> setPage(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String pageRef = request.param("pageRef");
        String pageTitle = request.param("pageTitle");
        proxy.newPage(pageRef, pageTitle);

        return Reply.saying().ok();
    }

    @Get
    @At("/:port/blacklist")
    public Reply<?> getBlacklist(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        return Reply.with(proxy.getBlacklistedUrls()).as(Json.class);
    }

    @Put
    @At("/:port/blacklist")
    public Reply<?> blacklist(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
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
    public Reply<?> clearBlacklist(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.clearBlacklist();
        return Reply.saying().ok();
    }

    @Get
    @At("/:port/whitelist")
    public Reply<?> getWhitelist(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        return Reply.with(proxy.getWhitelistUrls()).as(Json.class);
    }

    @Put
    @At("/:port/whitelist")
    public Reply<?> whitelist(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
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
    public Reply<?> clearWhitelist(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.clearWhitelist();
        return Reply.saying().ok();
    }

    @Post
    @At("/:port/auth/basic/:domain")
    public Reply<?> autoBasicAuth(@Named("port") int port, @Named("domain") String domain, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Map<String, String> credentials = request.read(HashMap.class).as(Json.class);
        proxy.autoBasicAuthorization(domain, credentials.get("username"), credentials.get("password"));

        return Reply.saying().ok();
    }

    @Post
    @At("/:port/headers")
    public Reply<?> updateHeaders(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        Map<String, String> headers = request.read(Map.class).as(Json.class);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            proxy.addHeader(key, value);
        }
        return Reply.saying().ok();
    }

    @Post
    @At("/:port/filter/request")
    public Reply<?> addRequestFilter(@Named("port") int port, Request<String> request) throws IOException, ScriptException {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
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
    public Reply<?> addResponseFilter(@Named("port") int port, Request<String> request) throws IOException, ScriptException {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
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
    @At("/:port/timeout")
    public Reply<?> timeout(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String requestTimeout = request.param("requestTimeout");
        if (requestTimeout != null) {
            try {
                proxy.setRequestTimeout(Integer.parseInt(requestTimeout));
            } catch (NumberFormatException e) {
            }
        }
        String readTimeout = request.param("readTimeout");
        if (readTimeout != null) {
            try {
                proxy.setSocketOperationTimeout(Integer.parseInt(readTimeout));
            } catch (NumberFormatException e) {
            }
        }
        String connectionTimeout = request.param("connectionTimeout");
        if (connectionTimeout != null) {
            try {
                proxy.setConnectionTimeout(Integer.parseInt(connectionTimeout));
            } catch (NumberFormatException e) {
            }
        }
        String dnsCacheTimeout = request.param("dnsCacheTimeout");
        if (dnsCacheTimeout != null) {
            try {
                proxy.setDNSCacheTimeout(Integer.parseInt(dnsCacheTimeout));
            } catch (NumberFormatException e) {
            }
        }
        return Reply.saying().ok();
    }

    @Delete
    @At("/:port")
    public Reply<?> delete(@Named("port") int port) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxyManager.delete(port);
        return Reply.saying().ok();
    }

    @Post
    @At("/:port/hosts")
    public Reply<?> remapHosts(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        @SuppressWarnings("unchecked") Map<String, String> headers = request.read(Map.class).as(Json.class);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            proxy.remapHost(key, value);
            proxy.setDNSCacheTimeout(0);
            proxy.clearDNSCache();
        }

        return Reply.saying().ok();
    }


    @Put
    @At("/:port/wait")
    public Reply<?> wait(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String quietPeriodInMs = request.param("quietPeriodInMs");
        String timeoutInMs = request.param("timeoutInMs");
        proxy.waitForNetworkTrafficToStop(Integer.parseInt(quietPeriodInMs), Integer.parseInt(timeoutInMs));
        return Reply.saying().ok();
    }

    @Delete
    @At("/:port/dns/cache")
    public Reply<?> clearDnsCache(@Named("port") int port) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.clearDNSCache();
        return Reply.saying().ok();
    }

    @Put
    @At("/:port/rewrite")
    public Reply<?> rewriteUrl(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
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
    public Reply<?> clearRewriteRules(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        proxy.clearRewriteRules();
        return Reply.saying().ok();
    }

    @Put
    @At("/:port/retry")
    public Reply<?> retryCount(@Named("port") int port, Request<String> request) {
        BrowserMobProxyServerLegacyAdapter proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        String count = request.param("retrycount");
        proxy.setRetryCount(Integer.parseInt(count));
        return Reply.saying().ok();
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

    private String getEntityBodyFromRequest(Request<String> request) throws IOException {
        String contentTypeHeader = request.header("Content-Type");
        Charset charset = null;
        try {
            charset = BrowserMobHttpUtil.readCharsetInContentTypeHeader(contentTypeHeader);
        } catch (UnsupportedCharsetException e) {
            java.nio.charset.UnsupportedCharsetException cause = e.getUnsupportedCharsetExceptionCause();
            LOG.error("Character set declared in Content-Type header is not supported. Content-Type header: {}", contentTypeHeader, cause);

            throw cause;
        }

        if (charset == null) {
            charset = BrowserMobHttpUtil.DEFAULT_HTTP_CHARSET;
        }

        ByteArrayOutputStream entityBodyBytes = new ByteArrayOutputStream();
        request.readTo(entityBodyBytes);

        return new String(entityBodyBytes.toByteArray(), charset);
    }

}
