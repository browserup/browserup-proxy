package com.browserup.bup.proxy.guice;

import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.rest.validation.mapper.ConstraintViolationExceptionMapper;
import com.browserup.bup.rest.filter.LoggingFilter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.models.OpenAPI;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

public class JettyServerProvider implements Provider<Server> {
    public static final String SWAGGER_CONFIG_NAME = "swagger-config.yaml";
    public static final String SWAGGER_PACKAGE = "com.browserup.bup.rest.resource";

    private Server server;

    private AbstractBinder proxyManagerToHkBinder(ProxyManager proxyManager) {
        Factory<ProxyManager> proxyManagerFactory = new Factory<>() {

            @Override
            public ProxyManager provide() {
                return proxyManager;
            }

            @Override
            public void dispose(ProxyManager instance) {}
        };

        return new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(proxyManagerFactory).to(ProxyManager.class);
            }
        };
    }

    @Inject
    public JettyServerProvider(@Named("port") int port, @Named("address") String address, ProxyManager proxyManager) throws UnknownHostException {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages(SWAGGER_PACKAGE);
        OpenApiResource openApiResource = new OpenApiResource();
        openApiResource.setConfigLocation(SWAGGER_CONFIG_NAME);
        resourceConfig.register(openApiResource);
        resourceConfig.register(proxyManagerToHkBinder(proxyManager));
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(ConstraintViolationExceptionMapper.class);
        resourceConfig.registerClasses(LoggingFilter.class);

        resourceConfig.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, true);

        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder sh = new ServletHolder(servletContainer);

        server = new Server(new InetSocketAddress(InetAddress.getByName(address), port));

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        context.addFilter(GuiceFilter.class, "/*", 0);
        context.addServlet(DefaultServlet.class, "/");
        context.addServlet(sh, "/*");


        server.setHandler(context);
    }

    @Override
    public Server get() {
        return server;
    }
}
