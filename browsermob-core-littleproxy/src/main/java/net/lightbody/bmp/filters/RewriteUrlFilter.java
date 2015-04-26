package net.lightbody.bmp.filters;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.proxy.RewriteRule;
import org.littleshoot.proxy.HttpFiltersAdapter;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;

/**
 * Applies rewrite rules to the specified request. If a rewrite rule matches, the request's URI will be overwritten with the rewritten URI.
 * The filter does not make a defensive copy of the rewrite rule collection, so there is no guarantee
 * that the collection at the time of construction will contain the same values when the filter is actually invoked, if the collection is
 * modified concurrently.
 */
public class RewriteUrlFilter extends HttpFiltersAdapter {
    private final Collection<RewriteRule> rewriteRules;

    public RewriteUrlFilter(HttpRequest originalRequest, Collection<RewriteRule> rewriterules) {
        super(originalRequest);

        if (rewriterules != null) {
            this.rewriteRules = rewriterules;
        } else {
            this.rewriteRules = Collections.emptyList();
        }
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) httpObject;

            String uri = httpRequest.getUri();
            boolean rewroteUri = false;
            for (RewriteRule rule : rewriteRules) {
                Matcher matcher = rule.getPattern().matcher(uri);
                if (matcher.matches()) {
                    uri = matcher.replaceAll(rule.getReplace());
                    rewroteUri = true;
                }
            }

            if (rewroteUri) {
                httpRequest.setUri(uri);
            }
        }

        return null;
    }
}
