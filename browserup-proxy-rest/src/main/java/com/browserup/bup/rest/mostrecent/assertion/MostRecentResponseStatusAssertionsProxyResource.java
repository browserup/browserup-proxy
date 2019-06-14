//package com.browserup.bup.rest.mostrecent.assertion;
//
//import com.browserup.bup.BrowserUpProxyServer;
//import com.browserup.bup.assertion.model.AssertionResult;
//import com.browserup.bup.proxy.ProxyManager;
//import com.browserup.bup.rest.mostrecent.MostRecentEntryProxyResource;
//import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
//import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
//import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
//import com.browserup.bup.util.HttpStatusClass;
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
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//
//@Path("/proxy/{port}/har/mostRecentEntry")
//public class MostRecentResponseStatusAssertionsProxyResource extends MostRecentEntryProxyResource {
//    private static final Logger LOG = LoggerFactory.getLogger(MostRecentResponseStatusAssertionsProxyResource.class);
//
//    private ValidatedParam<Integer> status = ValidatedParam.empty("status");
//
//    public MostRecentResponseStatusAssertionsProxyResource(ProxyManager proxyManager) {
//        super(proxyManager);
//    }
//
//    public void setStatus(String status) {
//        this.status = parseIntParam(new StringRawParam("status", status));
//    }
//
//    @GET
//    @Path("/assertStatusEquals")
//    public AssertionResult statusEquals(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusEquals");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(proxy, status);
//        checkOptionalParams(urlPattern);
//
//        BrowserUpProxyServer proxyServer = proxy.getParsedParam();
//
//        return urlPattern.isEmpty() ?
//                proxyServer.assertMostRecentResponseStatusCode(status.getParsedParam()) :
//                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), status.getParsedParam());
//    }
//
//    @GET
//    @Path("/assertStatusInformational")
//    public AssertionResult statusInformational(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusInformational");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(proxy);
//        checkOptionalParams(urlPattern);
//
//        BrowserUpProxyServer proxyServer = proxy.getParsedParam();
//
//        return urlPattern.isEmpty() ?
//                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.INFORMATIONAL) :
//                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.INFORMATIONAL);
//    }
//
//    @GET
//    @Path("/assertStatusSuccess")
//    public AssertionResult statusSuccess(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusSuccess");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(proxy);
//        checkOptionalParams(urlPattern);
//
//        BrowserUpProxyServer proxyServer = proxy.getParsedParam();
//
//        return urlPattern.isEmpty() ?
//                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS) :
//                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.SUCCESS);
//    }
//
//    @GET
//    @Path("/assertStatusRedirection")
//    public AssertionResult statusRedirection(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusRedirection");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(proxy);
//        checkOptionalParams(urlPattern);
//
//        BrowserUpProxyServer proxyServer = proxy.getParsedParam();
//
//        return urlPattern.isEmpty() ?
//                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.REDIRECTION) :
//                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.REDIRECTION);
//    }
//
//    @GET
//    @Path("/assertStatusClientError")
//    public AssertionResult statusClientError(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusClientError");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(proxy);
//        checkOptionalParams(urlPattern);
//
//        BrowserUpProxyServer proxyServer = proxy.getParsedParam();
//
//        return urlPattern.isEmpty() ?
//                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.CLIENT_ERROR) :
//                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.CLIENT_ERROR);
//    }
//
//
//    @GET
//    @Path("/assertStatusServerError")
//    public AssertionResult statusServerError(@PathParam("port") int port) {
//        LOG.info("GET /" + port + "/har/mostRecentEntry/assertStatusServerError");
//
//        proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(proxy);
//        checkOptionalParams(urlPattern);
//
//        BrowserUpProxyServer proxyServer = proxy.getParsedParam();
//
//        return urlPattern.isEmpty() ?
//                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SERVER_ERROR) :
//                proxyServer.assertMostRecentResponseStatusCode(urlPattern.getParsedParam(), HttpStatusClass.SERVER_ERROR);
//    }
//}
