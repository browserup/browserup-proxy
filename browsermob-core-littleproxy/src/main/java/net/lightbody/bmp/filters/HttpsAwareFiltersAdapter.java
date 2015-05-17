package net.lightbody.bmp.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.littleshoot.proxy.HttpFiltersAdapter;

/**
 * The HttpsAwareFiltersAdapter exposes the original host and the "real" host (after filter modifications) to filters for HTTPS
 * requets. HTTPS requests do not normally contain the host in the URI, and the Host header may be missing or spoofed.
 * <p/>
 * <b>Note:</b> The {@link #getRequestHostAndPort()} and {@link #getOriginalRequestHostAndPort()} methods can only be
 * called when the request is an HTTPS request. Otherwise they will throw an IllegalStateException.
 */
public class HttpsAwareFiltersAdapter extends HttpFiltersAdapter {
    public static final String IS_HTTPS_ATTRIBUTE_NAME = "isHttps";
    public static final String HOST_ATTRIBUTE_NAME = "host";
    public static final String ORIGINAL_HOST_ATTRIBUTE_NAME = "originalHost";

    public HttpsAwareFiltersAdapter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
    }

    /**
     * Returns true if this is an HTTPS request.
     *
     * @return true if https, false if http
     */
    public boolean isHttps() {
        Attribute<Boolean> isHttpsAttr = ctx.attr(AttributeKey.<Boolean>valueOf(IS_HTTPS_ATTRIBUTE_NAME));

        Boolean isHttps = isHttpsAttr.get();
        if (isHttps == null) {
            return false;
        } else {
            return isHttps;
        }
    }

    /**
     * Returns the host and port of this HTTPS request, including any modifications by other filters.
     *
     * @return host and port of this HTTPS request
     * @throws IllegalStateException if this is not an HTTPS request
     */
    public String getRequestHostAndPort() throws IllegalStateException {
        if (!isHttps()) {
            throw new IllegalStateException("Request is not HTTPS. Cannot get host and port on non-HTTPS request using this method.");
        }

        Attribute<String> hostnameAttr = ctx.attr(AttributeKey.<String>valueOf(HOST_ATTRIBUTE_NAME));
        return hostnameAttr.get();
    }

    /**
     * Returns the original host and port of this HTTPS request, as sent by the client. Does not reflect any modifications
     * by other filters.
     *
     * @return host and port of this HTTPS request
     * @throws IllegalStateException if this is not an HTTPS request
     */
    public String getOriginalRequestHostAndPort() throws IllegalStateException {
        if (!isHttps()) {
            throw new IllegalStateException("Request is not HTTPS. Cannot get original host and port on non-HTTPS request using this method.");
        }

        Attribute<String> hostnameAttr = ctx.attr(AttributeKey.<String>valueOf(ORIGINAL_HOST_ATTRIBUTE_NAME));
        return hostnameAttr.get();
    }
}
