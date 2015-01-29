package net.lightbody.bmp.proxy.test.util;

import net.lightbody.bmp.proxy.EchoPayloadServlet;
import net.lightbody.bmp.proxy.EchoServlet;
import net.lightbody.bmp.proxy.JsonServlet;
import net.lightbody.bmp.proxy.SetCookieServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.GzipHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalServer {
    private int port;
    private Server server;
    private ResourceHandler handler;

    private static Logger log = LoggerFactory.getLogger(LocalServer.class);

    public void start() {
        server = new Server(0);

        HandlerList handlers = new HandlerList();

        ServletHandler handler = new ServletHandler();

        handler.addServletWithMapping(JsonServlet.class, "/jsonrpc");
        handler.addServletWithMapping(SetCookieServlet.class, "/cookie");
        handler.addServletWithMapping(EchoServlet.class, "/echo");
        handler.addServletWithMapping(EchoPayloadServlet.class, "/echopayload");

        handlers.addHandler(handler);

        ContextHandler contextHandler = new ContextHandler();
        contextHandler.setContextPath("/");
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newClassPathResource("/dummy-server"));
        contextHandler.setHandler(resourceHandler);

        handlers.addHandler(contextHandler);

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setMinGzipSize(Integer.MAX_VALUE);
        gzipHandler.setHandler(handlers);

        server.setHandler(gzipHandler);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("Could not start Jetty server", e);
        }

        this.port = server.getConnectors()[0].getLocalPort();
    }

    public int getPort() {
        return this.port;
    }

    public void enableGzip() {
        GzipHandler gzipHandler = (GzipHandler) server.getHandler();
        gzipHandler.setMinGzipSize(1);
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.error("Could not stop test server", e);
        }
    }

}
