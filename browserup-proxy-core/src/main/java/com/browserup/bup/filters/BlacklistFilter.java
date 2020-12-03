/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import com.browserup.bup.proxy.BlacklistEntry;
import com.browserup.bup.util.HttpStatusClass;
import io.netty.handler.codec.http.HttpUtil;

import java.util.Collection;
import java.util.Collections;

/**
 * Applies blacklist entries to this request. The filter does not make a defensive copy of the blacklist entries, so there is no guarantee
 * that the blacklist at the time of construction will contain the same values when the filter is actually invoked, if the entries are modified concurrently.
 */
public class BlacklistFilter extends HttpsAwareFiltersAdapter {
    public static final String BLOCKED_PHRASE = "Request blocked";
    private final Collection<BlacklistEntry> blacklistedUrls;

    public BlacklistFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, Collection<BlacklistEntry> blacklistedUrls) {
        super(originalRequest, ctx);

        if (blacklistedUrls != null) {
            this.blacklistedUrls = blacklistedUrls;
        } else {
            this.blacklistedUrls = Collections.emptyList();
        }
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) httpObject;

            String url = getOriginalUrl();

            for (BlacklistEntry entry : blacklistedUrls) {
                if (HttpMethod.CONNECT.equals(httpRequest.method()) && entry.getHttpMethodPattern() == null) {
                    // do not allow CONNECTs to be blacklisted unless a method pattern is explicitly specified
                    continue;
                }

                if (entry.matches(url, httpRequest.method().name())) {
                    HttpResponseStatus status;
                    if(HttpStatusClass.UNKNOWN.equals(HttpStatusClass.valueOf(entry.getStatusCode()))) {
                        status = new HttpResponseStatus(entry.getStatusCode(), BLOCKED_PHRASE);
                    } else {
                        status = HttpResponseStatus.valueOf(entry.getStatusCode());
                    }
                    HttpResponse resp = new DefaultFullHttpResponse(httpRequest.protocolVersion(), status);
                    HttpUtil.setContentLength(resp, 0L);

                    return resp;
                }
            }
        }

        return null;
    }
}
