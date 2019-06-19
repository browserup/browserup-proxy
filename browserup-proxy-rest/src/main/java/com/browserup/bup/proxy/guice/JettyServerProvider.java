package com.browserup.bup.proxy.guice;

import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.rest.BaseResource;
import com.browserup.bup.rest.CustomMapper;
import com.browserup.bup.rest.FooResource;
import com.browserup.bup.rest.filter.LoggingFilter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
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
        resourceConfig.packages(BaseResource.class.getPackageName());

        resourceConfig.registerClasses(OpenApiResource.class);
        resourceConfig.register(proxyManagerToHkBinder(proxyManager));
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(CustomMapper.class);
        resourceConfig.registerClasses(LoggingFilter.class);

        resourceConfig.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

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

//    public static ContextHandler buildSwaggerUI() throws Exception {
//        ResourceHandler rh = new ResourceHandler();
//        rh.setResourceBase(App.class.getClassLoader()
//                .getResource("META-INF/resources/webjars/swagger-ui/2.1.4")
//                .toURI().toString());
//        ContextHandler context = new ContextHandler();
//        context.setContextPath("/docs/");
//        context.setHandler(rh);
//        return context;
//    }

    @Override
    public Server get() {
        return server;
    }
}
