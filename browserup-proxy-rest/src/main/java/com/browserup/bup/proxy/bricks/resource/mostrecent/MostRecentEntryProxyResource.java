package com.browserup.bup.proxy.bricks.resource.mostrecent;

import com.browserup.bup.BrowserUpProxyServer;
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

import java.util.regex.Pattern;

@At("/proxy/:port/har/mostRecentEntry")
@Service
public class MostRecentEntryProxyResource extends BaseResource {
    private static final Logger LOG = LoggerFactory.getLogger(MostRecentEntryProxyResource.class);

    protected ValidatedParam<BrowserUpProxyServer> proxy = ValidatedParam.empty();
    protected ValidatedParam<Pattern> urlPattern = ValidatedParam.empty();
    protected ValidatedParam<Long> milliseconds = ValidatedParam.empty();

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = parsePatternParam(new StringRawParam("urlPattern", urlPattern));
    }

    public void setMilliseconds(String milliseconds) {
        this.milliseconds = parseLongParam(new StringRawParam("milliseconds", milliseconds));;
    }

    @Inject
    public MostRecentEntryProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    @Get
    public Reply<?> get(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy);

        HarEntry result = proxy.getParsedParam()
                .findMostRecentEntry(urlPattern.getParsedParam())
                .orElse(new HarEntry());

        return Reply.with(result).as(Json.class);
    }
}
