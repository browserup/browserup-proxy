package com.browserup.bup.proxy.bricks.resource.entries.assertion;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.resource.mostrecent.MostRecentEntryProxyResource;
import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
import com.browserup.bup.util.HttpStatusClass;
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

@At("/proxy/:port/har/entries")
@Service
public class EntriesResponseStatusAssertionsProxyResource extends MostRecentEntryProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(EntriesResponseStatusAssertionsProxyResource.class);

    private ValidatedParam<Integer> status = ValidatedParam.empty("status");

    @Inject
    public EntriesResponseStatusAssertionsProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    public void setStatus(String status) {
        this.status = parseIntParam(new StringRawParam("status", status));
    }

    @Get
    @At("/assertStatusEquals")
    public Reply<?> statusEquals(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertStatusEquals");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy, status);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertResponseStatusCode(status.getParsedParam()) :
                proxyServer.assertResponseStatusCode(urlPattern.getParsedParam(), status.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusInformational")
    public Reply<?> statusInformational(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertStatusInformational");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertResponseStatusCode(HttpStatusClass.INFORMATIONAL) :
                proxyServer.assertResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.INFORMATIONAL);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusSuccess")
    public Reply<?> statusSuccess(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertStatusSuccess");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertResponseStatusCode(HttpStatusClass.SUCCESS) :
                proxyServer.assertResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.SUCCESS);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusRedirection")
    public Reply<?> statusRedirection(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertStatusRedirection");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertResponseStatusCode(HttpStatusClass.REDIRECTION) :
                proxyServer.assertResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.REDIRECTION);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusClientError")
    public Reply<?> statusClientError(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertStatusClientError");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertResponseStatusCode(HttpStatusClass.CLIENT_ERROR) :
                proxyServer.assertResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.CLIENT_ERROR);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }


    @Get
    @At("/assertStatusServerError")
    public Reply<?> statusServerError(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertStatusServerError");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertResponseStatusCode(HttpStatusClass.SERVER_ERROR) :
                proxyServer.assertResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.SERVER_ERROR);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }
}
