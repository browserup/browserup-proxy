package com.browserup.bup.rest.mostrecent;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.rest.BaseResource;
import com.browserup.bup.rest.validation.*;
import com.browserup.bup.util.HttpStatusClass;
import com.browserup.harreader.model.HarEntry;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.regex.Pattern;

@Path("/proxy/{port}/har/mostRecentEntry")
public class MostRecentEntryProxyResource extends BaseResource {
    private static final String URL_PATTERN = "urlPattern";
    private static final String PORT = "port";
    private static final String CONTENT_TEXT = "contentText";
    private static final String CONTENT_PATTERN = "contentPattern";
    private static final String LENGTH = "length";
    private static final String MILLISECONDS = "milliseconds";
    private static final String HEADER_NAME = "headerName";
    private static final String HEADER_VALUE = "headerValue";
    private static final String HEADER_NAME_PATTERN = "headerNamePattern";
    private static final String HEADER_VALUE_PATTERN = "headerValuePattern";
    private static final String STATUS = "status";

    public MostRecentEntryProxyResource(@Context ProxyManager proxyManager) {
        super(proxyManager);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response mostRecentEntry(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern) {
        return Response.ok(proxyManager.get(port)
                .findMostRecentEntry(Pattern.compile(urlPattern))
                .orElse(new HarEntry())).build();
    }

    @GET
    @Path("/assertContentContains")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contentContains(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern,

            @QueryParam(CONTENT_TEXT)
            @NotBlankConstraint(paramName = CONTENT_TEXT)
            @Parameter(required = true) String contentText) {
        AssertionResult result = proxyManager.get(port)
                .assertMostRecentResponseContentContains(Pattern.compile(urlPattern), contentText);

        return Response.ok(result).build();
    }


    @GET
    @Path("/assertContentDoesNotContain")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contentDoesNotContain(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern,

            @QueryParam(CONTENT_TEXT)
            @NotBlankConstraint(paramName = CONTENT_TEXT)
            @Parameter(required = true) String contentText) {
        AssertionResult result = proxyManager.get(port)
                .assertMostRecentResponseContentDoesNotContain(Pattern.compile(urlPattern), contentText);

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertContentMatches")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contentMatches(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern,

            @QueryParam(CONTENT_PATTERN)
            @NotBlankConstraint(paramName = CONTENT_PATTERN)
            @PatternConstraint(paramName = CONTENT_PATTERN)
            @Parameter(required = true) String contentPattern) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseContentMatches(
                Pattern.compile(urlPattern),
                Pattern.compile(contentPattern));

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertContentLengthLessThanOrEqual")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contentLengthLessThanOrEqual(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern,

            @QueryParam(LENGTH)
            @NotNullConstraint(paramName = LENGTH)
            @LongPositiveConstraint(value = 0, paramName = LENGTH) String length) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseContentLengthLessThanOrEqual(
                Pattern.compile(urlPattern),
                Long.parseLong(length));

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertResponseTimeLessThanOrEqual")
    @Produces(MediaType.APPLICATION_JSON)
    public Response responseTimeLessThanOrEqual(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern,

            @QueryParam(MILLISECONDS)
            @NotNullConstraint(paramName = PORT)
            @Parameter(required = true) long milliseconds) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseTimeLessThanOrEqual(
                Pattern.compile(urlPattern),
                milliseconds);

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertResponseHeaderContains")
    @Produces(MediaType.APPLICATION_JSON)
    public Response responseHeaderContains(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern,

            @QueryParam(HEADER_NAME)
            @Parameter String headerName,

            @QueryParam(HEADER_VALUE)
            @NotBlankConstraint(paramName = HEADER_VALUE)
            @Parameter(required = true) String headerValue) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseHeaderContains(
                Pattern.compile(urlPattern),
                headerName, headerValue);

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertResponseHeaderDoesNotContain")
    @Produces(MediaType.APPLICATION_JSON)
    public Response responseHeaderDoesNotContain(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern,

            @QueryParam(HEADER_NAME)
            @Parameter String headerName,

            @QueryParam(HEADER_VALUE)
            @NotBlankConstraint(paramName = HEADER_VALUE)
            @Parameter(required = true) String headerValue) {
        AssertionResult result = proxyManager.get(port).assertMostRecentResponseHeaderDoesNotContain(
                Pattern.compile(urlPattern),
                headerName, headerValue);

        return Response.ok(result).build();
    }

    @GET
    @Path("/assertResponseHeaderMatches")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult responseHeaderMatches(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true) String urlPattern,

            @QueryParam(HEADER_NAME_PATTERN)
            @PatternConstraint(paramName = HEADER_NAME_PATTERN)
            @Parameter(required = true) String headerNamePattern,

            @QueryParam(HEADER_VALUE_PATTERN)
            @NotBlankConstraint(paramName = HEADER_VALUE_PATTERN)
            @PatternConstraint(paramName = HEADER_VALUE_PATTERN)
            @Parameter(required = true) String headerValuePattern) {
        return proxyManager.get(port).assertMostRecentResponseHeaderMatches(
                Pattern.compile(urlPattern),
                headerNamePattern != null ? Pattern.compile(headerNamePattern) : null,
                Pattern.compile(headerValuePattern));
    }

    @GET
    @Path("/assertStatusEquals")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusEquals(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter() String urlPattern,

            @QueryParam(STATUS)
            @NotNullConstraint(paramName = STATUS)
            @HttpStatusCodeConstraint(paramName = STATUS)
            @Parameter(required = true) String status) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);
        int intStatus = Integer.parseInt(status);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(intStatus) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), intStatus);
    }

    @GET
    @Path("/assertStatusInformational")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusInformational(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter() String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.INFORMATIONAL) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.INFORMATIONAL);
    }

    @GET
    @Path("/assertStatusSuccess")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusSuccess(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter() String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.SUCCESS);
    }

    @GET
    @Path("/assertStatusRedirection")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusRedirection(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter() String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.REDIRECTION) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.REDIRECTION);
    }

    @GET
    @Path("/assertStatusClientError")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusClientError(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter() String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.CLIENT_ERROR) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.CLIENT_ERROR);
    }

    @GET
    @Path("/assertStatusServerError")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult statusServerError(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter() String urlPattern) {

        BrowserUpProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SERVER_ERROR) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.SERVER_ERROR);
    }
}
