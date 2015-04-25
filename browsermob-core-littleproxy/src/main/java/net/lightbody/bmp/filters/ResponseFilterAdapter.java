package net.lightbody.bmp.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpObjectUtil;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

/**
 * A filter adapter for {@link ResponseFilter} implementations. Executes the filter when the {@link HttpFilters#serverToProxyResponse(HttpObject)}
 * method is invoked.
 */
public class ResponseFilterAdapter extends HttpFiltersAdapter {
    private final ResponseFilter responseFilter;

    private HttpResponse httpResponse;

    public ResponseFilterAdapter(HttpRequest originalRequest, ChannelHandlerContext ctx, ResponseFilter responseFilter) {
        super(originalRequest, ctx);

        this.responseFilter = responseFilter;
    }

    public ResponseFilterAdapter(HttpRequest originalRequest, ResponseFilter responseFilter) {
        super(originalRequest);

        this.responseFilter = responseFilter;
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            // technically TODO we don't need to do this...
            this.httpResponse = (HttpResponse) httpObject;
        }

        if (httpObject instanceof FullHttpMessage) {
            FullHttpMessage httpContent = (FullHttpMessage) httpObject;

            HttpMessageContents contents = new HttpMessageContents(httpContent);
            responseFilter.filterResponse(httpResponse, contents);
        }

        return super.serverToProxyResponse(httpObject);
    }

    /**
     * A {@link HttpFiltersSourceAdapter} for {@link ResponseFilterAdapter}s.
     */
    public static class FilterSource extends HttpFiltersSourceAdapter {
        private static final int DEFAULT_MAXIMUM_RESPONSE_BUFFER_SIZE = 2097152;

        private final ResponseFilter filter;
        private final int maximumResponseBufferSizeInBytes;

        /**
         * Creates a new filter source that will invoke the specified filter and uses the {@link #DEFAULT_MAXIMUM_RESPONSE_BUFFER_SIZE} as
         * the maximum buffer size.
         *
         * @param filter ResponseFilter to invoke
         */
        public FilterSource(ResponseFilter filter) {
            this.filter = filter;
            this.maximumResponseBufferSizeInBytes = DEFAULT_MAXIMUM_RESPONSE_BUFFER_SIZE;
        }

        /**
         * Creates a new filter source that will invoke the specified filter and uses the maximumResponseBufferSizeInBytes as the maximum
         * buffer size.
         *
         * @param filter ResponseFilter to invoke
         * @param maximumResponseBufferSizeInBytes maximum buffer size when aggregating responses for filtering
         */
        public FilterSource(ResponseFilter filter, int maximumResponseBufferSizeInBytes) {
            this.filter = filter;
            this.maximumResponseBufferSizeInBytes = maximumResponseBufferSizeInBytes;
        }

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            return new ResponseFilterAdapter(originalRequest, ctx, filter);
        }

        @Override
        public int getMaximumResponseBufferSizeInBytes() {
            return maximumResponseBufferSizeInBytes;
        }
    }
}
