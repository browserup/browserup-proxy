package com.browserup.bup.rest.mostrecent;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.rest.BaseResource;
import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
import com.browserup.bup.rest.validation.PortWithExistingProxyConstraint;
import com.browserup.bup.rest.validation.UrlPatternConstraint;
import com.browserup.harreader.model.HarEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.regex.Pattern;

@Path("/proxy/{port}/har/mostRecentEntry")
public class MostRecentEntryProxyResource extends BaseResource {
    private static final Logger LOG = LoggerFactory.getLogger(MostRecentEntryProxyResource.class);

    protected ValidatedParam<BrowserUpProxyServer> proxy = ValidatedParam.empty("proxy");

    public MostRecentEntryProxyResource(@Context ProxyManager proxyManager) {
        super(proxyManager);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response mostRecentEntry(@PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
                                    @QueryParam("urlPattern") @NotNull @UrlPatternConstraint String urlPattern) {
        LOG.info("GET /" + port + "/har/mostRecentEntry");

        return Response.ok(proxyManager.get(port)
                .findMostRecentEntry(Pattern.compile(urlPattern))
                .orElse(new HarEntry())).build();
    }

    @GET
    @Path("/assertContentContains")
    @Produces(MediaType.APPLICATION_JSON)
    public AssertionResult contentContains(@PathParam("port") @NotNull @PortWithExistingProxyConstraint int port,
                                           @QueryParam("urlPattern") @NotNull @UrlPatternConstraint String urlPattern,
                                           @QueryParam("contentText") @NotBlank String contentText) {
        LOG.info("GET /" + port + "/har/mostRecentEntry/assertContentContains");

        return proxyManager.get(port).assertMostRecentResponseContentContains(Pattern.compile(urlPattern), contentText);
    }

}
