package com.browserup.bup.proxy.bricks.resource.mostrecent;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.RawParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
import com.browserup.harreader.model.HarEntry;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.sitebricks.At;
import com.google.sitebricks.client.transport.Json;
import com.google.sitebricks.headless.Reply;
import com.google.sitebricks.headless.Service;
import com.google.sitebricks.http.Get;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

@At("/proxy/:port/har/mostRecentEntry")
@Service
public class MostRecentEntryAssertionsProxyResource extends MostRecentEntryProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(MostRecentEntryAssertionsProxyResource.class);

    protected ValidatedParam<String> text = ValidatedParam.empty();
    protected ValidatedParam<Integer> status = ValidatedParam.empty();
    protected ValidatedParam<Long> length = ValidatedParam.empty();

    @Inject
    public MostRecentEntryAssertionsProxyResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    public void setText(String text) {
        StringRawParam param = new StringRawParam("text", text);
        if (StringUtils.isEmpty(text)) {
            this.text = new ValidatedParam<>(param, "Empty text param is invalid");
        }
        this.text = new ValidatedParam<>(param, text, null);
    }

    public void setStatus(String status) {
        this.status = parseIntParam(new StringRawParam("status", status));
    }

    public void setLength(String length) {
        this.length = parseLongParam(new StringRawParam("length", length));
    }

    @Get
    @At("/assertResponseTimeWithin")
    public Reply<?> responseTimeWithin(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseTimeWithin");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, milliseconds, proxy);

        AssertionResult result = proxy.getRequiredParsedParam().assertMostRecentUrlResponseTimeWithin(
                urlPattern.getRequiredParsedParam(),
                milliseconds.getRequiredParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentContains")
    public Reply<?> contentContains(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentContains");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, text);

        AssertionResult result = proxy.getRequiredParsedParam().assertUrlContentContains(
                urlPattern.getRequiredParsedParam(),
                text.getRequiredParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentDoesNotContain")
    public Reply<?> contentDoesNotContain(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentDoesNotContain");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, text);

        AssertionResult result = proxy.getRequiredParsedParam().assertUrlContentDoesNotContain(
                urlPattern.getRequiredParsedParam(),
                text.getRequiredParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertResponseHeaderContains")
    public Reply<?> responseHeaderContains(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseHeaderContains");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, text);

        AssertionResult result = proxy.getRequiredParsedParam().assertUrlResponseHeaderContains(
                urlPattern.getRequiredParsedParam(),
                text.getRequiredParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertResponseHeaderDoesNotContain")
    public Reply<?> responseHeaderDoesNotContain(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseHeaderDoesNotContain");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, text);

        AssertionResult result = proxy.getRequiredParsedParam().assertUrlResponseHeaderDoesNotContain(
                urlPattern.getRequiredParsedParam(),
                text.getRequiredParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertStatusEquals")
    public Reply<?> statusEquals(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusEquals");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, status);

        AssertionResult result = proxy.getRequiredParsedParam().assertUrlStatusEquals(
                urlPattern.getRequiredParsedParam(),
                status.getRequiredParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentLengthWithin")
    public Reply<?> contentLengthWithin(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentLengthWithin");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkParams(urlPattern, proxy, length);

        AssertionResult result = proxy.getRequiredParsedParam().assertUrlContentLengthWithin(
                urlPattern.getRequiredParsedParam(),
                length.getRequiredParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }
}
