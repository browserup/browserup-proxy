package com.browserup.bup.proxy.bricks.resource.mostrecent.assertion;

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

@At("/proxy/:port/har/mostRecentEntry")
@Service
public class MostRecentResponseStatusAssertionsProxyResource extends MostRecentEntryProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(MostRecentResponseStatusAssertionsProxyResource.class);

    private ValidatedParam<Integer> status = ValidatedParam.empty("status");

    @Inject
    public MostRecentResponseStatusAssertionsProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    public void setStatus(String status) {
        this.status = parseIntParam(new StringRawParam("status", status));
    }

    @Get
    @At("/assertStatusEquals")
    public Reply<?> statusEquals(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusEquals");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy, status);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(status.getParsedParam()) :
                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), status.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusInformational")
    public Reply<?> statusInformational(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusInformational");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.INFORMATIONAL) :
                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.INFORMATIONAL);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusSuccess")
    public Reply<?> statusSuccess(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusSuccess");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS) :
                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.SUCCESS);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusRedirection")
    public Reply<?> statusRedirection(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusRedirection");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.REDIRECTION) :
                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.REDIRECTION);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusClientError")
    public Reply<?> statusClientError(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusClientError");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.CLIENT_ERROR) :
                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.CLIENT_ERROR);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }


    @Get
    @At("/assertStatusServerError")
    public Reply<?> statusServerError(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusServerError");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(proxy);
        checkOptionalParams(urlPattern);

        BrowserUpProxyServer proxyServer = proxy.getParsedParam();

        AssertionResult result = urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SERVER_ERROR) :
                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.SERVER_ERROR);

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }
}
