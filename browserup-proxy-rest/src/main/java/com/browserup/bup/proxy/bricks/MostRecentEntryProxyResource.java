package com.browserup.bup.proxy.bricks;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.param.ValidatedParam;
import com.browserup.harreader.model.HarEntry;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.sitebricks.At;
import com.google.sitebricks.client.transport.Json;
import com.google.sitebricks.headless.Reply;
import com.google.sitebricks.headless.Request;
import com.google.sitebricks.headless.Service;
import com.google.sitebricks.http.Get;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

@At("/proxy/:port/har/mostRecentEntry")
@Service
public class MostRecentEntryProxyResource extends BaseResource {
    private static final Logger LOG = LoggerFactory.getLogger(MostRecentEntryProxyResource.class);

    private final ProxyManager proxyManager;

    @Inject
    public MostRecentEntryProxyResource(ProxyManager proxyManager, ProxyManager proxyManager1) {
        super(proxyManager);
        this.proxyManager = proxyManager1;
    }

    @Get
    public Reply<?> findMostRecentEntry(@Named("port") int port, Request<String> request) {
        LOG.info("GET /" + port + "/har/entry");

        BrowserUpProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Reply.saying().notFound();
        }

        return getUrlPattern().getParam().map(p -> proxy.findMostRecentEntry(p)
                .map(entry -> (Reply) Reply.with(entry).as(Json.class))
                .orElse(Reply.with(new HarEntry()).as(Json.class)))
                .orElse(getUrlPattern().getErrorReply().orElse(Reply.saying().error()));
    }

    @Get
    @At("/assertResponseTimeWithin")
    public Reply<?> mostRecentEntryAssertResponseTimeWithin(@Named("port") int port, Request<String> request) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseTimeWithin");

        ValidatedParam<BrowserUpProxyServer> proxyServer = getBrowserUpProxyServer(port);
        ValidatedParam<Pattern> urlPattern = getUrlPattern();
        ValidatedParam<Long> time = getMilliseconds();

        return getValidationError(urlPattern, time).orElseGet(() -> {
            AssertionResult result = proxyServer.getRequredParam().assertMostRecentUrlResponseTimeWithin(
                    urlPattern.getRequredParam(),
                    time.getRequredParam());
            return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
        });
    }
}
