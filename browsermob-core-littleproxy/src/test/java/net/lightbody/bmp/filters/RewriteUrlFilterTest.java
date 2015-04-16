package net.lightbody.bmp.filters;

import com.google.common.collect.ImmutableList;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.proxy.RewriteRule;
import org.junit.Test;

import java.util.Collection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RewriteUrlFilterTest {
    @Test
    public void testRewriteWithCaptureGroups() {
        HttpRequest request = mock(HttpRequest.class);

        when(request.getUri()).thenReturn("http://www.yahoo.com?param=someValue");

        Collection<RewriteRule> rewriteRules = ImmutableList.of(new RewriteRule("http://www\\.(yahoo|bing)\\.com\\?(\\w+)=(\\w+)", "http://www.google.com?originalDomain=$1&$2=$3"));

        RewriteUrlFilter filter = new RewriteUrlFilter(request, rewriteRules);
        filter.clientToProxyRequest(request);

        verify(request).setUri("http://www.google.com?originalDomain=yahoo&param=someValue");
    }

    @Test
    public void testRewriteMultipleMatches() {
        HttpRequest request = mock(HttpRequest.class);

        when(request.getUri()).thenReturn("http://www.yahoo.com?param=someValue");

        Collection<RewriteRule> rewriteRules = ImmutableList.of(
                new RewriteRule("http://www\\.yahoo\\.com\\?(\\w+)=(\\w+)", "http://www.bing.com?new$1=new$2"),
                new RewriteRule("http://www\\.(yahoo|bing)\\.com\\?(\\w+)=(\\w+)", "http://www.google.com?originalDomain=$1&$2=$3")
        );

        RewriteUrlFilter filter = new RewriteUrlFilter(request, rewriteRules);
        filter.clientToProxyRequest(request);

        verify(request).setUri("http://www.google.com?originalDomain=bing&newparam=newsomeValue");
    }
}