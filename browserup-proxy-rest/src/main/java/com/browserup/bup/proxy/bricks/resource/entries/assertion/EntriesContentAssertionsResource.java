package com.browserup.bup.proxy.bricks.resource.entries.assertion;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.resource.entries.EntriesProxyResource;
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

@At("/proxy/:port/har/entries")
@Service
public class EntriesContentAssertionsResource extends EntriesProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(EntriesContentAssertionsResource.class);

    private ValidatedParam<Pattern> contentPattern = ValidatedParam.empty("contentPattern");
    private ValidatedParam<String> contentText = ValidatedParam.empty("contentText");
    private ValidatedParam<Long> length = ValidatedParam.empty("length");

    @Inject
    public EntriesContentAssertionsResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

    public void setContentText(String text) {
        this.contentText = parseNonEmptyStringParam(new StringRawParam("contentText", text));
    }

    public void setLength(String length) {
        this.length = parseLongParam(new StringRawParam("length", length));
    }

    public void setContentPattern(String contentPattern) {
        this.contentPattern = parsePatternParam(new StringRawParam("contentPattern", contentPattern));
    }

    @Get
    @At("/assertContentContains")
    public Reply<?> contentContains(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertContentContains");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(urlPattern, proxy, contentText);

        AssertionResult result = proxy.getParsedParam().assertAnyUrlContentContains(
                urlPattern.getParsedParam(),
                contentText.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentDoesNotContain")
    public Reply<?> contentDoesNotContain(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertContentDoesNotContain");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(urlPattern, proxy, contentText);

        AssertionResult result = proxy.getParsedParam().assertAnyUrlContentDoesNotContain(
                urlPattern.getParsedParam(),
                contentText.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentMatches")
    public Reply<?> contentMatches(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertContentMatches");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(urlPattern, proxy, contentPattern);

        AssertionResult result = proxy.getParsedParam().assertAnyUrlContentMatches(
                urlPattern.getParsedParam(),
                contentPattern.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }

    @Get
    @At("/assertContentLengthLessThanOrEqual")
    public Reply<?> contentLengthLessThanOrEqual(@Named("port") int port) {
        LOG.info("GET /" + port + "/har/entries/assertContentLengthLessThanOrEqual");

        proxy = parseProxyServer(new IntRawParam("proxy port", port));
        checkRequiredParams(urlPattern, proxy, length);

        AssertionResult result = proxy.getParsedParam().assertAnyUrlContentLengthLessThanOrEquals(
                urlPattern.getParsedParam(),
                length.getParsedParam());

        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
    }
}
