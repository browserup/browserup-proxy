/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.filters;

import com.browserup.bup.util.HttpStatusClass;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import org.littleshoot.proxy.impl.ProxyUtils;

import java.util.Collection;
import java.util.regex.Pattern;

import static java.util.Collections.*;

/**
 * Checks this request against the allowlist, and returns the modified response if the request is not in the allowlist. The filter does not
 * make a defensive copy of the allowlist URLs, so there is no guarantee that the allowlist URLs at the time of construction will contain the
 * same values when the filter is actually invoked, if the URL collection is modified concurrently.
 */
public class AllowlistFilter extends HttpsAwareFiltersAdapter {
    private final boolean allowlistEnabled;
    private final int allowlistResponseCode;
    private final Collection<Pattern> allowlistUrls;

    public AllowlistFilter(HttpRequest originalRequest,
                           ChannelHandlerContext ctx,
                           boolean allowlistEnabled,
                           int allowlistResponseCode,
                           Collection<Pattern> allowlistUrls) {
        super(originalRequest, ctx);

        this.allowlistEnabled = allowlistEnabled;
        this.allowlistResponseCode = allowlistResponseCode;
        this.allowlistUrls = allowlistUrls != null ? allowlistUrls : emptyList();
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (!allowlistEnabled) {
            return null;
        }

        if (httpObject instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) httpObject;

            // do not allow HTTP CONNECTs to be short-circuited
            if (ProxyUtils.isCONNECT(httpRequest)) {
                return null;
            }

            boolean urlAllowlisted;

            String url = getOriginalUrl();

            urlAllowlisted = allowlistUrls.stream().anyMatch(pattern -> pattern.matcher(url).matches());

            if (!urlAllowlisted) {
                HttpResponseStatus status;
                if(HttpStatusClass.UNKNOWN.equals(HttpStatusClass.valueOf(allowlistResponseCode))) {
                    status = new HttpResponseStatus(allowlistResponseCode, BlocklistFilter.BLOCKED_PHRASE);
                } else {
                    status = HttpResponseStatus.valueOf(allowlistResponseCode);
                }
                HttpResponse resp = new DefaultFullHttpResponse(httpRequest.protocolVersion(), status);
                HttpUtil.setContentLength(resp, 0L);

                return resp;
            }
        }

        return null;
    }
}
