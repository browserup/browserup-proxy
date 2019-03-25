package com.browserup.bup.proxy.bricks.resource.entries;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.resource.BaseResource;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
import com.browserup.harreader.model.HarEntry;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.sitebricks.At;
import com.google.sitebricks.client.transport.Json;
import com.google.sitebricks.headless.Reply;
import com.google.sitebricks.headless.Service;
import com.google.sitebricks.http.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.regex.Pattern;

@At("/proxy/:port/har/entries")
@Service
public class EntriesProxyResource extends BaseResource {
    private static final Logger LOG = LoggerFactory.getLogger(EntriesProxyResource.class);

    private ValidatedParam<BrowserUpProxyServer> proxy = ValidatedParam.empty();
    private ValidatedParam<Pattern> urlPattern = ValidatedParam.empty();
    private ValidatedParam<Long> milliseconds = ValidatedParam.empty();

    @Inject
    public EntriesProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = parsePatternParam(new StringRawParam("urlPattern", urlPattern));
    }

    public void setMilliseconds(String milliseconds) {
        this.milliseconds = parseLongParam(new StringRawParam("milliseconds", milliseconds));;
    }

    public void setPort(Integer port) {
        System.out.println();
    }

    @Get
    public Reply<?> findEntries(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy);

        Collection<HarEntry> result = proxy.getParsedParam().findEntries(urlPattern.getParsedParam());

        return Reply.with(result).as(Json.class);
    }

    @Get
    @At("/assertResponseTimeWithin")
    public Reply<?> findEntriesAndAssertResponseTimeWithin(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertResponseTimeWithin");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, milliseconds, proxy);

        AssertionResult result = proxy.getParsedParam().assertAllUrlResponseTimesWithin(
                        urlPattern.getParsedParam(),
                        milliseconds.getParsedParam());

        return Reply.with(result).as(Json.class);
    }
}
