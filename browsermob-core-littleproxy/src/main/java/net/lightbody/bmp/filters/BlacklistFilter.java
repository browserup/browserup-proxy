package net.lightbody.bmp.filters;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.lightbody.bmp.proxy.BlacklistEntry;
import org.littleshoot.proxy.HttpFiltersAdapter;

import java.util.Collection;
import java.util.Collections;

/**
 * Applies blacklist entries to this request. The filter does not make a defensive copy of the blacklist entries, so there is no guarantee
 * that the blacklist at the time of construction will contain the same values when the filter is actually invoked, if the entries are modified concurrently.
 */
public class BlacklistFilter extends HttpFiltersAdapter {
    private final Collection<BlacklistEntry> blacklistedUrls;

    public BlacklistFilter(HttpRequest originalRequest, Collection<BlacklistEntry> blacklistedUrls) {
        super(originalRequest);

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

            for (BlacklistEntry entry : blacklistedUrls) {
                if (entry.matches(httpRequest.getUri(), httpRequest.getMethod().name())) {
                    HttpResponseStatus status = HttpResponseStatus.valueOf(entry.getStatusCode());
                    HttpResponse resp = new DefaultFullHttpResponse(httpRequest.getProtocolVersion(), status);
                    HttpHeaders.setContentLength(resp, 0L);

                    return resp;
                }
            }
        }

        return null;
    }
}
