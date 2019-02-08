package com.browserup.bup.filters;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.littleshoot.proxy.HttpFiltersAdapter;

import java.util.Collections;
import java.util.Map;

/**
 * Adds the headers specified in the constructor to this request. The filter does not make a defensive copy of the map, so there is no guarantee
 * that the map at the time of construction will contain the same values when the filter is actually invoked, if the map is modified concurrently.
 */
public class AddHeadersFilter extends HttpFiltersAdapter {
    private final Map<String, String> additionalHeaders;

    public AddHeadersFilter(HttpRequest originalRequest, Map<String, String> additionalHeaders) {
        super(originalRequest);

        if (additionalHeaders != null) {
            this.additionalHeaders = additionalHeaders;
        } else {
            this.additionalHeaders = Collections.emptyMap();
        }
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) httpObject;

            additionalHeaders.forEach((key, value) -> httpRequest.headers().add(key, value));
        }

        return null;
    }
}
