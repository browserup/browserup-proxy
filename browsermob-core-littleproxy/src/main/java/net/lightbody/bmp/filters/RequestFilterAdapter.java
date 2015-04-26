package net.lightbody.bmp.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.util.HttpMessageContents;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

/**
 * A filter adapter for {@link RequestFilter} implementations. Executes the filter when the {@link HttpFilters#clientToProxyRequest(HttpObject)}
 * method is invoked.
 */
public class RequestFilterAdapter extends HttpFiltersAdapter {
    private final RequestFilter requestFilter;

    private HttpRequest httpRequest;

    public RequestFilterAdapter(HttpRequest originalRequest, ChannelHandlerContext ctx, RequestFilter requestFilter) {
        super(originalRequest, ctx);

        this.requestFilter = requestFilter;
    }

    public RequestFilterAdapter(HttpRequest originalRequest, RequestFilter requestFilter) {
        super(originalRequest);

        this.requestFilter = requestFilter;
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            // technically TODO we don't need to do this...
            this.httpRequest = (HttpRequest) httpObject;
        }

        if (httpObject instanceof FullHttpMessage) {
            FullHttpMessage httpContent = (FullHttpMessage) httpObject;

            HttpMessageContents contents = new HttpMessageContents(httpContent);
            HttpResponse response = requestFilter.filterRequest(httpRequest, contents);
            if (response != null) {
                return response;
            }
        }

        return null;
    }

    /**
     * A {@link HttpFiltersSourceAdapter} for {@link RequestFilterAdapter}s.
     */
    public static class FilterSource extends HttpFiltersSourceAdapter {
        private static final int DEFAULT_MAXIMUM_REQUEST_BUFFER_SIZE = 2097152;

        private final RequestFilter filter;
        private final int maximumRequestBufferSizeInBytes;

        /**
         * Creates a new filter source that will invoke the specified filter and uses the {@link #DEFAULT_MAXIMUM_REQUEST_BUFFER_SIZE} as
         * the maximum buffer size.
         *
         * @param filter RequestFilter to invoke
         */
        public FilterSource(RequestFilter filter) {
            this.filter = filter;
            this.maximumRequestBufferSizeInBytes = DEFAULT_MAXIMUM_REQUEST_BUFFER_SIZE;
        }

        /**
         * Creates a new filter source that will invoke the specified filter and uses the maximumRequestBufferSizeInBytes as the maximum
         * buffer size.
         *
         * @param filter RequestFilter to invoke
         * @param maximumRequestBufferSizeInBytes maximum buffer size when aggregating Requests for filtering
         */
        public FilterSource(RequestFilter filter, int maximumRequestBufferSizeInBytes) {
            this.filter = filter;
            this.maximumRequestBufferSizeInBytes = maximumRequestBufferSizeInBytes;
        }

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            return new RequestFilterAdapter(originalRequest, ctx, filter);
        }

        @Override
        public int getMaximumRequestBufferSizeInBytes() {
            return maximumRequestBufferSizeInBytes;
        }
    }
}
