package com.browserup.bup.proxy.bricks;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.param.ValidatedParam;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.sitebricks.At;
import com.google.sitebricks.client.transport.Json;
import com.google.sitebricks.headless.Reply;
import com.google.sitebricks.headless.Service;
import com.google.sitebricks.http.Get;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

@At("/proxy/:port/har/entries")
@Service
public class EntriesProxyResource extends BaseResource {
    private static final Logger LOG = LoggerFactory.getLogger(EntriesProxyResource.class);

    @Inject
    public EntriesProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    @Get
    public Reply<?> findEntries(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries");

        ValidatedParam<BrowserUpProxyServer> proxyServer = getBrowserUpProxyServer(port);
        ValidatedParam<Pattern> urlPattern = getUrlPattern();

        return getValidationError(urlPattern, proxyServer)
                .orElse(
                        Reply.with(proxyServer.getRequredParam().findEntries(
                                urlPattern.getRequredParam())
                        ).as(Json.class));
    }

    @Get
    @At("/assertResponseTimeWithin")
    public Reply<?> findEntriesAndAssertResponseTimeWithin(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertResponseTimeWithin");

        ValidatedParam<BrowserUpProxyServer> proxyServer = getBrowserUpProxyServer(port);
        ValidatedParam<Pattern> urlPattern = getUrlPattern();
        ValidatedParam<Long> time = getMilliseconds();

        return getValidationError(urlPattern, time)
                .orElse(
                        Reply.with(proxyServer.getRequredParam().assertAllUrlsResponseTimeWithin(
                                urlPattern.getRequredParam(),
                                time.getRequredParam())
                        ).status(HttpStatus.OK_200).as(Json.class));

    }
}
