package com.browserup.bup.proxy.bricks.resource.entries.assertion;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.resource.BaseResource;
import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
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
public class EntriesAssertionsProxyResource extends BaseResource {
    private static final Logger LOG = LoggerFactory.getLogger(EntriesAssertionsProxyResource.class);

    private ValidatedParam<BrowserUpProxyServer> proxy = ValidatedParam.empty("proxy");
    private ValidatedParam<Pattern> urlPattern = ValidatedParam.empty("urlPattern");
    private ValidatedParam<Long> milliseconds = ValidatedParam.empty("milliseconds");

    @Inject
    public EntriesAssertionsProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = parsePatternParam(new StringRawParam("urlPattern", urlPattern));
    }

    public void setMilliseconds(String milliseconds) {
        this.milliseconds = parseLongParam(new StringRawParam("milliseconds", milliseconds));;
    }

    @Get
    @At("/assertResponseTimeUnder")
    public Reply<?> responseTimeUnder(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertResponseTimeUnder");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(urlPattern, milliseconds, proxy);

        AssertionResult result = proxy.getParsedParam().assertResponseTimeUnder(
                        urlPattern.getParsedParam(),
                        milliseconds.getParsedParam());

        return Reply.with(result).as(Json.class);
    }
}
