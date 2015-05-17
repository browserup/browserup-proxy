package net.lightbody.bmp.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.impl.ProxyUtils;

/**
 * Captures the original host for HTTPS requests and stores the value in the ChannelHandlerContext for use by {@link HttpsAwareFiltersAdapter}
 * filters. This filter sets the isHttps attribute on the ChannelHandlerContext during the HTTP CONNECT and therefore MUST be invoked before
 * any other filters calling any of the methods in {@link HttpsAwareFiltersAdapter}.
 */
public class HttpsOriginalHostCaptureFilter extends HttpFiltersAdapter {
    public HttpsOriginalHostCaptureFilter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);

        // if this is an HTTP CONNECT, set the isHttps attribute on the ChannelHandlerConect and capture the hostname from the original request.
        // capturing the original host (and the remapped/modified host in clientToProxyRequest() below) guarantees that we will
        // have the "true" host, rather than relying on the Host header in subsequent requests (which may be absent or spoofed by malicious clients).
        if (ProxyUtils.isCONNECT(originalRequest)) {
            Attribute<String> originalHostAttr = ctx.attr(AttributeKey.<String>valueOf(HttpsAwareFiltersAdapter.ORIGINAL_HOST_ATTRIBUTE_NAME));
            String hostAndPort = originalRequest.getUri();
            originalHostAttr.set(hostAndPort);

            Attribute<Boolean> isHttpsAttr = ctx.attr(AttributeKey.<Boolean>valueOf(HttpsAwareFiltersAdapter.IS_HTTPS_ATTRIBUTE_NAME));
            isHttpsAttr.set(true);
        }
    }
}
