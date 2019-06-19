package com.browserup.bup.rest.mostrecent;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.rest.BaseResource;
import com.browserup.bup.rest.validation.*;
import com.browserup.bup.util.HttpStatusClass;
import com.browserup.harreader.model.HarEntry;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.regex.Pattern;

@Path("/proxy/{port}/har/mostRecentEntry")
public class MostRecentEntryProxyResource extends BaseResource {

    public MostRecentEntryProxyResource(@Context ProxyManager proxyManager) {
        super(proxyManager);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response mostRecentEntry(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @NotBlank @PatternConstraint(paramName = "urlPattern") String urlPattern) {
        return Response.ok(proxyManager.get(port)
                .findMostRecentEntry(Pattern.compile(urlPattern))
                .orElse(new HarEntry())).build();
    }

    @GET
    @Path("/assertContentContains")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contentContains(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("contentText") @NotBlank String contentText) {
        AssertionResult result = proxyManager.get(port)
                .assertMostRecentResponseContentContains(Pattern.compile(urlPattern), contentText);

        return Response.ok(result).build();
    }


    @GET
    @Path("/assertContentDoesNotContain")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contentDoesNotContain(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @NotBlank @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("contentText") @NotBlank String contentText) {
        AssertionResult result = proxyManager.get(port)
                .assertMostRecentResponseContentDoesNotContain(Pattern.compile(urlPattern), contentText);

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertContentMatches")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contentMatches(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @NotBlank @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("contentPattern") @NotBlank @PatternConstraint(paramName = "contentPattern") String contentPattern) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseContentMatches(
                Pattern.compile(urlPattern),
                Pattern.compile(contentPattern));

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertContentLengthLessThanOrEqual")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contentLengthLessThanOrEqual(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @NotBlank @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("length") @NotNull @DecimalMin("0") String length) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseContentLengthLessThanOrEqual(
                Pattern.compile(urlPattern),
                Long.parseLong(length));

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertResponseTimeLessThanOrEqual")
    @Produces(MediaType.APPLICATION_JSON)
    public Response responseTimeLessThanOrEqual(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @NotBlank @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("milliseconds") @NotNull long milliseconds) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseTimeLessThanOrEqual(
                Pattern.compile(urlPattern),
                milliseconds);

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertResponseHeaderContains")
    @Produces(MediaType.APPLICATION_JSON)
    public Response responseHeaderContains(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @NotBlank @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("headerName") @NotBlank String headerName,
            @QueryParam("headerValue") @NotBlank String headerValue) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseHeaderContains(
                Pattern.compile(urlPattern),
                headerName, headerValue);

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertResponseHeaderDoesNotContain")
    @Produces(MediaType.APPLICATION_JSON)
    public Response responseHeaderDoesNotContain(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @NotBlank @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("headerName") @NotBlank String headerName,
            @QueryParam("headerValue") @NotBlank String headerValue) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseHeaderDoesNotContain(
                Pattern.compile(urlPattern),
                headerName, headerValue);

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertResponseHeaderMatches")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult responseHeaderMatches(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @NotBlank @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("headerNamePattern") @NotBlank @PatternConstraint(paramName = "headerNamePattern") String headerNamePattern,
            @QueryParam("headerValuePattern") @NotBlank  @PatternConstraint(paramName = "headerValuePattern") String headerValuePattern) {
        return proxyManager.get(port).assertMostRecentResponseHeaderMatches(
                Pattern.compile(urlPattern),
                Pattern.compile(headerNamePattern),
                Pattern.compile(headerValuePattern));
    }

    @GET
    @Path("/assertStatusEquals")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusEquals(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @PatternConstraint(paramName = "urlPattern") String urlPattern,
            @QueryParam("status") @NotNull int status) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(status) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), status);
    }

    @GET
    @Path("/assertStatusInformational")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusInformational(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @PatternConstraint(paramName = "urlPattern") String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.INFORMATIONAL) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.INFORMATIONAL);
    }

    @GET
    @Path("/assertStatusSuccess")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusSuccess(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @PatternConstraint(paramName = "urlPattern") String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.SUCCESS);
    }

    @GET
    @Path("/assertStatusRedirection")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusRedirection(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @PatternConstraint(paramName = "urlPattern") String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.REDIRECTION) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.REDIRECTION);
    }

    @GET
    @Path("/assertStatusClientError")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusClientError(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @PatternConstraint(paramName = "urlPattern") String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.CLIENT_ERROR) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.CLIENT_ERROR);
    }

    @GET
    @Path("/assertStatusServerError")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusServerError(
            @PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
            @QueryParam("urlPattern") @PatternConstraint(paramName = "urlPattern") String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SERVER_ERROR) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.SERVER_ERROR);
    }
}
