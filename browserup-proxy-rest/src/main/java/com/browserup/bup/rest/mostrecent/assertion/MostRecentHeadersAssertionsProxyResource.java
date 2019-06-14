//package com.browserup.bup.rest.mostrecent.assertion;
//
//import com.browserup.bup.assertion.model.AssertionResult;
//import com.browserup.bup.proxy.ProxyManager;
//import com.browserup.bup.rest.mostrecent.MostRecentEntryProxyResource;
//import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
//import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
//import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
//import org.eclipse.jetty.http.HttpStatus;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//import java.util.regex.Pattern;
//
//@Path("/proxy/{port}/har/mostRecentEntry")
//public class MostRecentHeadersAssertionsProxyResource extends MostRecentEntryProxyResource {
//    private static final Logger LOG = LoggerFactory.getLogger(MostRecentHeadersAssertionsProxyResource.class);
//
//    private ValidatedParam<String> headerName = ValidatedParam.empty("headerName");
//    private ValidatedParam<Pattern> headerNamePattern = ValidatedParam.empty("headerNamePattern");
//    private ValidatedParam<String> headerValue = ValidatedParam.empty("headerValue");
//    private ValidatedParam<Pattern> headerValuePattern = ValidatedParam.empty("headerValuePattern");
//
//    public MostRecentHeadersAssertionsProxyResource(ProxyManager proxyManager) {
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
//    @GET
//    @Path("/assertResponseHeaderContains")
//    public AssertionResult responseHeaderContains(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseHeaderContains");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(urlPattern, proxy, headerValue);
//
//        return proxy.getParsedParam().assertMostRecentResponseHeaderContains(
//                urlPattern.getParsedParam(),
//                headerName.getParsedParam(),
//                headerValue.getParsedParam());
//    }
//
//    @GET
//    @Path("/assertResponseHeaderDoesNotContain")
//    public AssertionResult responseHeaderDoesNotContain(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseHeaderDoesNotContain");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(urlPattern, proxy, headerValue);
//
//        return proxy.getParsedParam().assertMostRecentResponseHeaderDoesNotContain(
//                urlPattern.getParsedParam(),
//                headerName.getParsedParam(),
//                headerValue.getParsedParam());
//    }
//
//    @GET
//    @Path("/assertResponseHeaderDoesNotContain")
//    public AssertionResult responseHeaderMatches(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseHeaderMatches");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(urlPattern, proxy, headerValuePattern);
//
//        return proxy.getParsedParam().assertMostRecentResponseHeaderMatches(
//                urlPattern.getParsedParam(),
//                headerNamePattern.getParsedParam(),
//                headerValuePattern.getParsedParam());
//    }
//}
