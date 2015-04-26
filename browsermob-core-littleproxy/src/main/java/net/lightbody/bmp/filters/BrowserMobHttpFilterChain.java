package net.lightbody.bmp.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.lightbody.bmp.BrowserMobProxyServer;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The filter "driver" that delegates to all chained filters specified by the proxy server.
 */
public class BrowserMobHttpFilterChain extends HttpFiltersAdapter {
    private static final Logger log = LoggerFactory.getLogger(BrowserMobHttpFilterChain.class);

    private final BrowserMobProxyServer proxyServer;

    private final List<HttpFilters> filters;

    public BrowserMobHttpFilterChain(BrowserMobProxyServer proxyServer, HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);

        this.proxyServer = proxyServer;

        if (proxyServer.getFilterFactories() != null) {
            filters = new ArrayList<HttpFilters>(proxyServer.getFilterFactories().size());

            // instantiate all HttpFilters using the proxy's filter factories
            for (HttpFiltersSource filterFactory : proxyServer.getFilterFactories()) {
                HttpFilters filter = filterFactory.filterRequest(originalRequest, ctx);
                filters.add(filter);
            }
        } else {
            filters = Collections.emptyList();
        }
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (proxyServer.isStopped()) {
            log.warn("Aborting request to {} because proxy is stopped", originalRequest.getUri());
            return new DefaultHttpResponse(originalRequest.getProtocolVersion(), HttpResponseStatus.SERVICE_UNAVAILABLE);
        }

        for (HttpFilters filter : filters) {
            HttpResponse filterResponse = filter.clientToProxyRequest(httpObject);
            if (filterResponse != null) {
                return filterResponse;
            }
        }

        return null;
    }

    @Override
    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
        for (HttpFilters filter : filters) {
            HttpResponse filterResponse = filter.proxyToServerRequest(httpObject);
            if (filterResponse != null) {
                return filterResponse;
            }
        }

        return null;
    }

    @Override
    public void proxyToServerRequestSending() {
        for (HttpFilters filter : filters) {
            filter.proxyToServerRequestSending();
        }
    }


    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        HttpObject processedHttpObject = httpObject;

        for (HttpFilters filter : filters) {
            processedHttpObject = filter.serverToProxyResponse(processedHttpObject);
            if (processedHttpObject == null) {
                return null;
            }
        }

        return processedHttpObject;
    }

    @Override
    public void serverToProxyResponseReceiving() {
        for (HttpFilters filter : filters) {
            filter.serverToProxyResponseReceiving();
        }
    }

    @Override
    public InetSocketAddress proxyToServerResolutionStarted(String resolvingServerHostAndPort) {
        InetSocketAddress overrideAddress = null;
        String newServerHostAndPort = resolvingServerHostAndPort;

        for (HttpFilters filter : filters) {
            InetSocketAddress filterResult = filter.proxyToServerResolutionStarted(newServerHostAndPort);
            if (filterResult != null) {
                overrideAddress = filterResult;
                newServerHostAndPort = filterResult.getHostString() + ":" + filterResult.getPort();
            }
        }

        return overrideAddress;
    }

    @Override
    public void proxyToServerResolutionSucceeded(String serverHostAndPort, InetSocketAddress resolvedRemoteAddress) {
        for (HttpFilters filter : filters) {
            filter.proxyToServerResolutionSucceeded(serverHostAndPort, resolvedRemoteAddress);
        }

        super.proxyToServerResolutionSucceeded(serverHostAndPort, resolvedRemoteAddress);
    }

    @Override
    public void proxyToServerConnectionStarted() {
        for (HttpFilters filter : filters) {
            filter.proxyToServerConnectionStarted();
        }
    }

    @Override
    public void proxyToServerConnectionSSLHandshakeStarted() {
        for (HttpFilters filter : filters) {
            filter.proxyToServerConnectionSSLHandshakeStarted();
        }
    }

    @Override
    public void proxyToServerConnectionFailed() {
        for (HttpFilters filter : filters) {
            filter.proxyToServerConnectionFailed();
        }
    }

    @Override
    public void proxyToServerConnectionSucceeded() {
        for (HttpFilters filter : filters) {
            filter.proxyToServerConnectionSucceeded();
        }
    }

    @Override
    public void proxyToServerRequestSent() {
        for (HttpFilters filter : filters) {
            filter.proxyToServerRequestSent();
        }
    }

    @Override
    public void serverToProxyResponseReceived() {
        for (HttpFilters filter : filters) {
            filter.serverToProxyResponseReceived();
        }
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        HttpObject processedHttpObject = httpObject;
        for (HttpFilters filter : filters) {
            processedHttpObject = filter.proxyToClientResponse(processedHttpObject);
            if (processedHttpObject == null) {
                return null;
            }
        }

        return processedHttpObject;
    }

    @Override
    public void proxyToServerConnectionQueued() {
        for (HttpFilters filter : filters) {
            filter.proxyToServerConnectionQueued();
        }
    }
}
