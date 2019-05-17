package com.browserup.bup.proxy.bricks.resource.entries.assertion;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.resource.entries.EntriesProxyResource;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.sitebricks.At;
import com.google.sitebricks.client.transport.Json;
import com.google.sitebricks.headless.Reply;
import com.google.sitebricks.headless.Service;
import com.google.sitebricks.http.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@At("/proxy/:port/har/entries")
@Service
public class EntriesMediaResourcesAssertionsProxyResource extends EntriesProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(EntriesMediaResourcesAssertionsProxyResource.class);

    @Inject
    public EntriesMediaResourcesAssertionsProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    @Get
    @At("/assertImageResponsesSuccessful")
    public Reply<?> imageResponsesSuccessful(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertImageResponsesSuccessful");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);

        AssertionResult result = proxy.getParsedParam().assertImageResponsesSuccessful();

        return Reply.with(result).as(Json.class);
    }

    @Get
    @At("/assertJavaScriptResponsesSuccessful")
    public Reply<?> javaScriptResponsesSuccessful(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertJavaScriptResponsesSuccessful");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);

        AssertionResult result = proxy.getParsedParam().assertJavaScriptResponsesSuccessful();

        return Reply.with(result).as(Json.class);
    }

    @Get
    @At("/assertStyleSheetResponsesSuccessful")
    public Reply<?> styleSheetResponsesSuccessful(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertStyleSheetResponsesSuccessful");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);

        AssertionResult result = proxy.getParsedParam().assertStyleSheetResponsesSuccessful();

        return Reply.with(result).as(Json.class);
    }

    @Get
    @At("/assertResourceResponsesSuccessful")
    public Reply<?> resourceResponsesSuccessful(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertResourceResponsesSuccessful");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);

        AssertionResult result = proxy.getParsedParam().assertResourceResponsesSuccessful();

        return Reply.with(result).as(Json.class);
    }
}
