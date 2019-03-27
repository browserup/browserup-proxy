package com.browserup.bup.proxy.bricks.resource.mostrecent.assertion;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.resource.mostrecent.MostRecentEntryProxyResource;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
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

@At("/proxy/:port/har/mostRecentEntry")
@Service
public class MostRecentEntryAssertionsProxyResource extends MostRecentEntryProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(MostRecentEntryAssertionsProxyResource.class);

    @Inject
    public MostRecentEntryAssertionsProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    @Get
    @At("/assertResponseTimeUnder")
    public Reply<?> responseTimeUnder(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseTimeUnder");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(urlPattern, milliseconds, proxy);

        AssertionResult result = proxy.getParsedParam().assertMostRecentResponseTimeUnder(
                urlPattern.getParsedParam(),
                milliseconds.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }
}
