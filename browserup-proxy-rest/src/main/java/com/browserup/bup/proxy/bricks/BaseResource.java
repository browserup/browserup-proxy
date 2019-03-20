package com.browserup.bup.proxy.bricks;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.param.ValidatedParam;
import com.google.inject.Inject;
import com.google.sitebricks.client.transport.Text;
import com.google.sitebricks.headless.Reply;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BaseResource  {
    private static final Logger LOG = LoggerFactory.getLogger(EntriesProxyResource.class);

    private final ProxyManager proxyManager;

    private ValidatedParam<Pattern> urlPattern = ValidatedParam.empty();
    private ValidatedParam<Long> milliseconds = ValidatedParam.empty();

    @Inject
    public BaseResource(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    public void setUrlPattern(String urlParam) {
        try {
            urlPattern = new ValidatedParam<>(parseUrlPattern(urlParam));
        } catch (IllegalArgumentException ex) {
            Reply<?> reply = Reply.with(ex.getMessage()).badRequest().as(Text.class);
            urlPattern = new ValidatedParam<>(reply);
        }
    }

    public ValidatedParam<Pattern> getUrlPattern() {
        return urlPattern;
    }

    public void setMilliseconds(String ms) {
        try {
            milliseconds = new ValidatedParam<>(parseMilliseconds(ms));
        } catch (IllegalArgumentException ex) {
            Reply<?> reply = Reply.with(ex.getMessage()).badRequest().as(Text.class);
            milliseconds = new ValidatedParam<>(reply);
        }
    }

    public ValidatedParam<Long> getMilliseconds() {
        return milliseconds;
    }

    public ValidatedParam<BrowserUpProxyServer> getBrowserUpProxyServer(int port) {
        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return new ValidatedParam<>(Reply.with("No proxy server found for port").notFound());
        } else {
            return new ValidatedParam<>(proxy);
        }
    }

    protected Optional<Reply<?>> getValidationError(ValidatedParam<?>... params) {
        List<ValidatedParam<?>> failedParams = Arrays.stream(params)
                .filter(p -> !p.getParam().isPresent())
                .collect(Collectors.toList());
        if (!failedParams.isEmpty()) {
            return Optional.of(failedParams.get(0).getErrorReply().orElse(Reply.saying().badRequest()));
        } else {
            return Optional.empty();
        }
    }

    protected Pattern parseUrlPattern(String urlParam) throws IllegalArgumentException {
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

    protected Long parseMilliseconds(String msParam) throws IllegalArgumentException {
        if (StringUtils.isEmpty(msParam)) {
            LOG.warn("Time parameter not present");
            throw new IllegalArgumentException("URL parameter 'milliseconds' is mandatory");
        }

        Long time;
        try {
            time = Long.valueOf(msParam);
        } catch (Exception ex) {
            LOG.warn("Time parameter not valid", ex);
            throw new IllegalArgumentException("URL parameter 'milliseconds' is invalid");
        }
        return time;
    }
}
