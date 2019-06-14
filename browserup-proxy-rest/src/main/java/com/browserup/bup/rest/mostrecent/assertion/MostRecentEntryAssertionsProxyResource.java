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
//
//@Path("/proxy/{port}/har/mostRecentEntry")
//public class MostRecentEntryAssertionsProxyResource extends MostRecentEntryProxyResource {
//    private static final Logger LOG = LoggerFactory.getLogger(MostRecentEntryAssertionsProxyResource.class);
//
//    protected ValidatedParam<Long> milliseconds = ValidatedParam.empty("milliseconds");
//
//    public MostRecentEntryAssertionsProxyResource(ProxyManager proxyManager) {
//        super(proxyManager);
//    }
//
//    public void setMilliseconds(String milliseconds) {
//        this.milliseconds = parseLongParam(new StringRawParam("milliseconds", milliseconds));;
//    }
//
//    @GET
//    @Path("/assertResponseTimeLessThanOrEqual")
//    public AssertionResult responseTimeLessThanOrEqual(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertResponseTimeLessThanOrEqual");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(urlPattern, milliseconds, proxy);
//
//        return proxy.getParsedParam().assertMostRecentResponseTimeLessThanOrEqual(
//                urlPattern.getParsedParam(),
//                milliseconds.getParsedParam());
//    }
//}
