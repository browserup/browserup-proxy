package com.browserup.bup.proxy.bricks.resource.mostrecent;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
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

import java.util.regex.Pattern;

@At("/proxy/:port/har/mostRecentEntry")
@Service
public class MostRecentEntryAssertionsProxyResource extends MostRecentEntryProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(MostRecentEntryAssertionsProxyResource.class);

    private ValidatedParam<Pattern> contentPattern = ValidatedParam.empty();
    private ValidatedParam<String> contentText = ValidatedParam.empty();

    private ValidatedParam<String> headerName = ValidatedParam.empty();
    private ValidatedParam<Pattern> headerNamePattern = ValidatedParam.empty();
    private ValidatedParam<String> headerValue = ValidatedParam.empty();
    private ValidatedParam<Pattern> headerValuePattern = ValidatedParam.empty();

    private ValidatedParam<Integer> status = ValidatedParam.empty();
    private ValidatedParam<Long> length = ValidatedParam.empty();

    @Inject
    public MostRecentEntryAssertionsProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    public void setContentText(String text) {
        this.contentText = parseNonEmptyStringParam(new StringRawParam("contentText", text));
    }

    public void setStatus(String status) {
        this.status = parseIntParam(new StringRawParam("status", status));
    }

    public void setLength(String length) {
        this.length = parseLongParam(new StringRawParam("length", length));
    }

    public void setContentPattern(String contentPattern) {
        this.contentPattern = parsePatternParam(new StringRawParam("contentPattern", contentPattern));
    }

    public void setHeaderName(String headerName) {
        this.headerName = parseNonEmptyStringParam(new StringRawParam("headerName", headerName));
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = parseNonEmptyStringParam(new StringRawParam("headerValue", headerValue));
    }

    public void setHeaderNamePattern(String headerNamePattern) {
        this.headerNamePattern = parsePatternParam(new StringRawParam("headerNamePattern", headerNamePattern));
    }

    public void setHeaderValuePattern(String headerValuePattern) {
        this.headerValuePattern = parsePatternParam(new StringRawParam("headerValuePattern", headerValuePattern));
    }

    @Get
    @At("/assertResponseTimeWithin")
    public Reply<?> responseTimeWithin(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseTimeWithin");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, milliseconds, proxy);

        AssertionResult result = proxy.getParsedParam().assertMostRecentUrlResponseTimeWithin(
                urlPattern.getParsedParam(),
                milliseconds.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentContains")
    public Reply<?> contentContains(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentContains");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, contentText);

        AssertionResult result = proxy.getParsedParam().assertUrlContentContains(
                urlPattern.getParsedParam(),
                contentText.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentDoesNotContain")
    public Reply<?> contentDoesNotContain(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentDoesNotContain");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, contentText);

        AssertionResult result = proxy.getParsedParam().assertUrlContentDoesNotContain(
                urlPattern.getParsedParam(),
                contentText.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentMatches")
    public Reply<?> contentMatches(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentMatches");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, contentPattern);

        AssertionResult result = proxy.getParsedParam().assertUrlContentMatches(
                urlPattern.getParsedParam(),
                contentPattern.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertResponseHeaderContains")
    public Reply<?> responseHeaderContains(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseHeaderContains");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, headerValue);

        AssertionResult result = proxy.getParsedParam().assertUrlResponseHeaderContains(
                urlPattern.getParsedParam(),
                headerName.getParsedParam(),
                headerValue.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertResponseHeaderDoesNotContain")
    public Reply<?> responseHeaderDoesNotContain(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseHeaderDoesNotContain");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, headerValue);

        AssertionResult result = proxy.getParsedParam().assertUrlResponseHeaderDoesNotContain(
                urlPattern.getParsedParam(),
                headerName.getParsedParam(),
                headerValue.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertResponseHeaderMatches")
    public Reply<?> responseHeaderMatches(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseHeaderMatches");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, headerValuePattern);

        AssertionResult result = proxy.getParsedParam().assertUrlResponseHeaderMatches(
                urlPattern.getParsedParam(),
                headerNamePattern.getParsedParam(),
                headerValuePattern.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusEquals")
    public Reply<?> statusEquals(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusEquals");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, status);

        AssertionResult result = proxy.getParsedParam().assertMostRecentResponseStatusCode(
                urlPattern.getParsedParam(),
                status.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentLengthWithin")
    public Reply<?> contentLengthWithin(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentLengthWithin");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, length);

        AssertionResult result = proxy.getParsedParam().assertUrlContentLengthUnder(
                urlPattern.getParsedParam(),
                length.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }
}
