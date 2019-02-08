package com.browserup.bup.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import com.browserup.bup.proxy.ActivityMonitor;
import org.littleshoot.proxy.HttpFiltersAdapter;

/**
 * Registers this request with the {@link com.browserup.bup.proxy.ActivityMonitor} when the HttpRequest is received from the client.
 */
public class RegisterRequestFilter extends HttpFiltersAdapter {
    private final ActivityMonitor activityMonitor;

    public RegisterRequestFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, ActivityMonitor activityMonitor) {
        super(originalRequest, ctx);

        this.activityMonitor = activityMonitor;
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            activityMonitor.requestStarted();
        }

        return super.clientToProxyRequest(httpObject);
    }
}
