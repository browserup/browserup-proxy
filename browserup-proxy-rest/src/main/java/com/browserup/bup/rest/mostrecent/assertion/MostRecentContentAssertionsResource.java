package com.browserup.bup.rest.mostrecent.assertion;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.rest.mostrecent.MostRecentEntryProxyResource;
import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;

import com.browserup.bup.rest.validation.PortWithExistingProxyConstraint;
import com.browserup.bup.rest.validation.UrlPatternConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.regex.Pattern;

//@Path("/proxy/{port}/har/mostRecentEntry/***")
public class MostRecentContentAssertionsResource extends MostRecentEntryProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(MostRecentContentAssertionsResource.class);

    public MostRecentContentAssertionsResource(ProxyManager proxyManager) {
        super(proxyManager);
    }

//    @GET
//    @Path("/assertContentContains")
//    @Produces(MediaType.APPLICATION_JSON)
//    public AssertionResult contentContains(@PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
//                                           @QueryParam("urlPattern") @NotNull @UrlPatternConstraint String urlPattern,
//                                           @QueryParam("contentText") @NotBlank String contentText) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentContains");
//
//        return proxyManager.get(port).assertMostRecentResponseContentContains(Pattern.compile(urlPattern), contentText);
//    }
//
//    @GET
//    @Path("/assertContentDoesNotContain")
//    public AssertionResult contentDoesNotContain(@PathParam("port") int port,
//                                                 @QueryParam("contentText") String rawContentText,
//                                                 @QueryParam("urlPattern") String rawUrlPattern) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentDoesNotContain");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        ValidatedParam<String> contentText = parseNonEmptyStringParam(new StringRawParam("contentText", rawContentText));
//        ValidatedParam<Pattern> urlPattern = parsePatternParam(new StringRawParam("urlPattern", rawUrlPattern));
//        checkRequiredParams(urlPattern, proxy, contentText);
//
//        return proxy.getParsedParam().assertMostRecentResponseContentDoesNotContain(
//                urlPattern.getParsedParam(),
//                contentText.getParsedParam());
//    }
//
//    @GET
//    @Path("/assertContentDoesNotContain")
//    public AssertionResult contentMatches(@PathParam("port") int port,
//                                          @QueryParam("contentPattern") String rawContentPattern,
//                                          @QueryParam("urlPattern") String rawUrlPattern) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentMatches");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        ValidatedParam<Pattern> contentPattern = parsePatternParam(new StringRawParam("contentPattern", rawContentPattern));
//        ValidatedParam<Pattern> urlPattern = parsePatternParam(new StringRawParam("urlPattern", rawUrlPattern));
//        checkRequiredParams(urlPattern, proxy, contentPattern);
//
//        return proxy.getParsedParam().assertMostRecentResponseContentMatches(
//                urlPattern.getParsedParam(),
//                contentPattern.getParsedParam());
//    }
//
//    @GET
//    @Path("/assertContentLengthLessThanOrEqual")
//    public AssertionResult contentLengthLessThanOrEqual(@PathParam("port") int port,
//                                                        @QueryParam("length") String rawLength,
//                                                        @QueryParam("urlPattern") String rawUrlPattern) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentLengthLessThanOrEqual");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        ValidatedParam<Long> length = parseLongParam(new StringRawParam("length", rawLength));
//        ValidatedParam<Pattern> urlPattern = parsePatternParam(new StringRawParam("urlPattern", rawUrlPattern));
//        checkRequiredParams(urlPattern, proxy, length);
//
//        return proxy.getParsedParam().assertMostRecentResponseContentLengthLessThanOrEqual(
//                urlPattern.getParsedParam(),
//                length.getParsedParam());
//    }
}
