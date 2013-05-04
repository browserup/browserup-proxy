package net.lightbody.bmp.proxy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.sitebricks.SitebricksModule;
import net.lightbody.bmp.proxy.bricks.ProxyResource;
import net.lightbody.bmp.proxy.guice.ConfigModule;
import net.lightbody.bmp.proxy.guice.JettyModule;
import net.lightbody.bmp.proxy.util.Log;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletContextEvent;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    private static final Log LOG = new Log();

    public static void main(String[] args) throws Exception {
        String version = "UNKNOWN/DEVELOPMENT";
        InputStream is = Main.class.getResourceAsStream("/META-INF/maven/net.lightbody.bmp/browsermob-proxy/pom.properties");
        if (is != null) {
            Properties props = new Properties();
            props.load(is);
            version = props.getProperty("version");
        }

        final Injector injector = Guice.createInjector(new ConfigModule(args), new JettyModule(), new SitebricksModule() {
            @Override
            protected void configureSitebricks() {
                scan(ProxyResource.class.getPackage());
            }
        });

        LOG.info("Starting BrowserMob Proxy version %s", version);

        Server server = injector.getInstance(Server.class);
        GuiceServletContextListener gscl = new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return injector;
            }
        };
        server.start();

        ServletContextHandler context = (ServletContextHandler) server.getHandler();
        gscl.contextInitialized(new ServletContextEvent(context.getServletContext()));

        server.join();
    }
}
