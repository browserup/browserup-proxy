package com.browserup.bup.proxy.bricks.resource.mostrecent.assertion;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.resource.mostrecent.MostRecentEntryProxyResource;
import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
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

    protected ValidatedParam<Long> milliseconds = ValidatedParam.empty("milliseconds");

    @Inject
    public MostRecentEntryAssertionsProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    public void setMilliseconds(String milliseconds) {
        this.milliseconds = parseLongParam(new StringRawParam("milliseconds", milliseconds));;
    }

    @Get
    @At("/assertResponseTimeLessThanOrEqual")
    public Reply<?> responseTimeLessThanOrEqual(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseTimeLessThanOrEqual");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(urlPattern, milliseconds, proxy);

        AssertionResult result = proxy.getParsedParam().assertMostRecentResponseTimeLessThanOrEqual(
                urlPattern.getParsedParam(),
                milliseconds.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }
}
