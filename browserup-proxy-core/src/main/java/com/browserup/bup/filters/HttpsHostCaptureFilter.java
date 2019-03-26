package com.browserup.bup.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import com.browserup.bup.util.BrowserUpHttpUtil;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.impl.ProxyUtils;

import static com.browserup.bup.filters.HttpsAwareFiltersAdapter.*;

/**
 * Captures the host for HTTPS requests and stores the value in the ChannelHandlerContext for use by {@link HttpsAwareFiltersAdapter}
 * filters. This filter reads the host from the HttpRequest during the HTTP CONNECT call, and therefore MUST be invoked
 * <b>after</b> any other filters which modify the host.
 * Note: If the request uses the default HTTPS port (443), it will be removed from the hostname captured by this filter.
 */
public class HttpsHostCaptureFilter extends HttpFiltersAdapter {
    public HttpsHostCaptureFilter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) httpObject;

            if (ProxyUtils.isCONNECT(httpRequest)) {
                Attribute<String> hostname = ctx.channel().attr(AttributeKey.valueOf(HOST_ATTRIBUTE_NAME));
                String hostAndPort = httpRequest.uri();

                // CONNECT requests contain the port, even when using the default port. a sensible default is to remove the
                // default port, since in most cases it is not explicitly specified and its presence (in a HAR file, for example)
                // would be unexpected.
                String hostNoDefaultPort = BrowserUpHttpUtil.removeMatchingPort(hostAndPort, 443);
                hostname.set(hostNoDefaultPort);
            }
        }

        return null;
    }
}
