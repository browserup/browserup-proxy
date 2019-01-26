package net.lightbody.bmp.proxy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.sitebricks.SitebricksModule;
import net.lightbody.bmp.exception.JettyException;
import net.lightbody.bmp.proxy.bricks.ProxyResource;
import net.lightbody.bmp.proxy.guice.ConfigModule;
import net.lightbody.bmp.proxy.guice.JettyModule;
import net.lightbody.bmp.util.BrowserMobProxyUtil;
import net.lightbody.bmp.util.DeleteDirectoryTask;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    // the static final logger is in this static inner class to allow the logging configuration code to execute before the logger is initialized.
    // since the 'log' field is in a static inner class, it will only be initialized when it is actually used, instead of when the classloader
    // loads the Main class.
    private static class LogHolder {
        private static final Logger log = LoggerFactory.getLogger(Main.class);
    }

    private static final String BMP_LOG_CONFIG_NAME = "bmp-logging.yaml";
    private static final String DEFAULT_LOG_CONFIG_LOCATION = "bin/conf/" + BMP_LOG_CONFIG_NAME;

    public static final String LOG4J_CONFIGURATION_FILE_PROPERTY = "log4j.configurationFile";

    public static void main(String[] args) {
        configureLogging();

        if (args.length > 0 && "--version".equals(args[0])) {
            System.out.println("BrowserMob Proxy " + BrowserMobProxyUtil.getVersionString());
            System.exit(0);
        }

        final Injector injector = Guice.createInjector(new ConfigModule(args), new JettyModule(), new SitebricksModule() {
            @Override
            protected void configureSitebricks() {
                scan(ProxyResource.class.getPackage());
            }
        });

        LogHolder.log.info("Starting BrowserMob Proxy version {}", BrowserMobProxyUtil.getVersionString());

        Server server = injector.getInstance(Server.class);
        GuiceServletContextListener gscl = new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return injector;
            }
        };
        try {
            server.start();
        } catch (Exception e) {
            LogHolder.log.error("Failed to start Jetty server. Aborting.", e);

            throw new JettyException("Unable to start Jetty server", e);
        }

        ServletContextHandler context = (ServletContextHandler) server.getHandler();
        gscl.contextInitialized(new ServletContextEvent(context.getServletContext()));

        try {
            server.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Configures logging when running the proxy in stand-alone mode. Searches for a configuration file in the following order:
     * <ol>
     * <li>The file specified in the log4j.configurationFile system property, if defined</li>
     * <li>The bmp-logging.yaml file in (basedir)/bin/conf, if the basedir system property has been defined</li>
     * <li>The bmp-logging.yaml file in the bin/conf directory relative to the current working directory</li>
     * <li>The bmp-logging.yaml file in the current working directory</li>
     * <li>The bmp-logging.yaml file on the classpath</li>
     * </ol>
     */
    private static void configureLogging() {
        // allow users to override the log4j config file location. if the log4j.configurationFile property has been set, don't attempt to override it
        // with BMP's logging configuration.
        String log4jFileLocation = System.getProperty(LOG4J_CONFIGURATION_FILE_PROPERTY);
        if (log4jFileLocation == null || log4jFileLocation.isEmpty()) {
            // user is not specifying a log file, so look for one on the filesystem.
            // the system property "basedir" is set to the location of the browsermob-proxy script or .bat file by the script itself.
            String basedir = System.getProperty("basedir");
            // if basedir is not defined, attempt to resolve the file relative to the working directory
            if (basedir == null) {
                basedir = "";
            }

            Path baseDirPath = Paths.get(basedir);

            Path logFilePath = baseDirPath.resolve(DEFAULT_LOG_CONFIG_LOCATION);

            // if the config file is not readable, attempt to find the config file in the current working directory
            if (!Files.isReadable(logFilePath)) {
                logFilePath = Paths.get(BMP_LOG_CONFIG_NAME);

                // if the config file is still not readable, fall back to the config file on the classpath.
                if (!Files.isReadable(logFilePath)) {

                    InputStream defaultLogConfigStream = Main.class.getResourceAsStream("/" + BMP_LOG_CONFIG_NAME);
                    if (defaultLogConfigStream == null) {
                        // can't find the default log config file. give up.
                        return;
                    }

                    Path tempDir;
                    try {
                        tempDir = Files.createTempDirectory("browsermob-proxy");
                    } catch (IOException e) {
                        // can't create the temp directory, so nowhere to put the config file. give up.
                        return;
                    }

                    // delete the temp directory when the VM stops or aborts
                    Runtime.getRuntime().addShutdownHook(new Thread(new DeleteDirectoryTask(tempDir)));

                    // copy the default config file to the temp directory from the classpath
                    logFilePath = tempDir.resolve(BMP_LOG_CONFIG_NAME);
                    try {
                        Files.copy(defaultLogConfigStream, logFilePath);
                    } catch (IOException e) {
                        // can't copy the file. give up.
                        return;
                    }
                }
            }

            try {
                // convert the path to a URL, to avoid a MalformedURLException with log4j 2 on Windows
                System.setProperty("log4j.configurationFile", logFilePath.toAbsolutePath().toFile().toURI().toURL().toString());
            } catch (MalformedURLException | RuntimeException e) {
                System.out.println("Could not set log4j.configurationFile to " + logFilePath.toAbsolutePath().toString() + " due to error: "  + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
 
