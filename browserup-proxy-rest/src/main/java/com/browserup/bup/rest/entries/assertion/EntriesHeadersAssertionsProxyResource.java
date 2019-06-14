//package com.browserup.bup.rest.entries.assertion;
//
//import com.browserup.bup.assertion.model.AssertionResult;
//import com.browserup.bup.proxy.ProxyManager;
//import com.browserup.bup.rest.entries.EntriesProxyResource;
//import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
//import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
//import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
//import com.google.inject.Inject;
//import com.google.inject.name.Named;
//import com.google.sitebricks.At;
//import com.google.sitebricks.client.transport.Json;
//import com.google.sitebricks.headless.Reply;
//import com.google.sitebricks.headless.Service;
//import com.google.sitebricks.http.Get;
//import org.eclipse.jetty.http.HttpStatus;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.regex.Pattern;
//
//@At("/proxy/:port/har/entries")
//@Service
//public class EntriesHeadersAssertionsProxyResource extends EntriesProxyResource {
//    private static final Logger LOG = LoggerFactory.getLogger(EntriesHeadersAssertionsProxyResource.class);
//
//    private ValidatedParam<String> headerName = ValidatedParam.empty("headerName");
//    private ValidatedParam<Pattern> headerNamePattern = ValidatedParam.empty("headerNamePattern");
//    private ValidatedParam<String> headerValue = ValidatedParam.empty("headerValue");
//    private ValidatedParam<Pattern> headerValuePattern = ValidatedParam.empty("headerValuePattern");
//
//    @Inject
//    public EntriesHeadersAssertionsProxyResource(ProxyManager proxyManager) {
//        super(proxyManager);
//    }
//
//    public void setHeaderName(String headerName) {
//        this.headerName = parseNonEmptyStringParam(new StringRawParam("headerName", headerName));
//    }
//
//    public void setHeaderValue(String headerValue) {
//        this.headerValue = parseNonEmptyStringParam(new StringRawParam("headerValue", headerValue));
//    }
//
//    public void setHeaderNamePattern(String headerNamePattern) {
//        this.headerNamePattern = parsePatternParam(new StringRawParam("headerNamePattern", headerNamePattern));
//    }
//
//    public void setHeaderValuePattern(String headerValuePattern) {
//        this.headerValuePattern = parsePatternParam(new StringRawParam("headerValuePattern", headerValuePattern));
//    }
//
//    @Get
//    @At("/assertResponseHeaderContains")
//    public Reply<?> responseHeaderContains(@Named("port") int port) {
//        LOG.info("GET /" + port + "/har/entries/assertResponseHeaderContains");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(urlPattern, proxy, headerValue);
//
//        AssertionResult result = proxy.getParsedParam().assertAnyUrlResponseHeaderContains(
//                urlPattern.getParsedParam(),
//                headerName.getParsedParam(),
//                headerValue.getParsedParam());
//
//        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
//    }
//
//    @Get
//    @At("/assertResponseHeaderDoesNotContain")
//    public Reply<?> responseHeaderDoesNotContain(@Named("port") int port) {
//        LOG.info("GET /" + port + "/har/entries/assertResponseHeaderDoesNotContain");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(urlPattern, proxy, headerValue);
//
//        AssertionResult result = proxy.getParsedParam().assertAnyUrlResponseHeaderDoesNotContain(
//                urlPattern.getParsedParam(),
//                headerName.getParsedParam(),
//                headerValue.getParsedParam());
//
//        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
//    }
//
//    @Get
//    @At("/assertResponseHeaderMatches")
//    public Reply<?> responseHeaderMatches(@Named("port") int port) {
//        LOG.info("GET /" + port + "/har/entries/assertResponseHeaderMatches");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(urlPattern, proxy, headerValuePattern);
//        checkOptionalParams(headerNamePattern);
//
//        AssertionResult result = proxy.getParsedParam().assertAnyUrlResponseHeaderMatches(
//                urlPattern.getParsedParam(),
//                headerNamePattern.getParsedParam(),
//                headerValuePattern.getParsedParam());
//
//        return Reply.with(result).status(HttpStatus.OK_200).as(Json.class);
//    }
//}
