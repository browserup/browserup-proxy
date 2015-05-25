package net.lightbody.bmp.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Encapsulates additional HTTP message data passed to request and response filters.
 */
public class HttpMessageInfo {
    private final HttpRequest originalRequest;
    private final ChannelHandlerContext channelHandlerContext;
    private final boolean isHttps;
    private final String originalUrl;

    public HttpMessageInfo(HttpRequest originalRequest, ChannelHandlerContext channelHandlerContext, boolean isHttps, String originalUrl) {
        this.originalRequest = originalRequest;
        this.channelHandlerContext = channelHandlerContext;
        this.isHttps = isHttps;
        this.originalUrl = originalUrl;
    }

    /**
     * The original request from the client. Does not reflect any modifications from previous filters.
     */
    public HttpRequest getOriginalRequest() {
        return originalRequest;
    }

    /**
     * The {@link ChannelHandlerContext} for this request's client connection.
     */
    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    /**
     * Returns true if this is an HTTPS message.
     */
    public boolean isHttps() {
        return isHttps;
    }

    /**
     * Returns the full, absolute URL of the original request from the client for both HTTP and HTTPS URLs. The URL
     * will not reflect modifications from this or other filters.
     */
    public String getOriginalUrl() {
        return originalUrl;
    }
}
