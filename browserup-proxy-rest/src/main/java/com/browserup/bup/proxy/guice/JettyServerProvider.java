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
import java.util.EnumSet;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
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

import javax.servlet.DispatcherType;

public class JettyServerProvider implements Provider<Server> {
    public static final String SWAGGER_CONFIG_NAME = "swagger-config.yaml";
    public static final String SWAGGER_PACKAGE = "com.browserup.bup.rest.resource";

    private Server server;

    @Inject
    public JettyServerProvider(@Named("port") int port, @Named("address") String address, ProxyManager proxyManager) throws UnknownHostException {
        OpenApiResource openApiResource = new OpenApiResource();
        openApiResource.setConfigLocation(SWAGGER_CONFIG_NAME);

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages(SWAGGER_PACKAGE);
        resourceConfig.register(openApiResource);
        resourceConfig.register(proxyManagerToHkBinder(proxyManager));
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(ConstraintViolationExceptionMapper.class);
        resourceConfig.registerClasses(LoggingFilter.class);

        resourceConfig.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, true);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        context.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
        context.addServlet(DefaultServlet.class, "/");
        context.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");

        server = new Server(new InetSocketAddress(InetAddress.getByName(address), port));
        server.setHandler(context);
    }

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

    @Override
    public Server get() {
        return server;
    }
}
