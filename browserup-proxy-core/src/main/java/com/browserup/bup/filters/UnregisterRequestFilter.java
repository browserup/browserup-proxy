package com.browserup.bup.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import com.browserup.bup.proxy.ActivityMonitor;
import org.littleshoot.proxy.HttpFiltersAdapter;

/**
 * Unregisters this request with the {@link com.browserup.bup.proxy.ActivityMonitor} when the LastHttpContent is sent to the client.
 */
public class UnregisterRequestFilter extends HttpFiltersAdapter {
    private final ActivityMonitor activityMonitor;

    public UnregisterRequestFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, ActivityMonitor activityMonitor) {
        super(originalRequest, ctx);

        this.activityMonitor = activityMonitor;
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        if (httpObject instanceof LastHttpContent) {
            activityMonitor.requestFinished();
        }

        return super.proxyToClientResponse(httpObject);
    }
}
