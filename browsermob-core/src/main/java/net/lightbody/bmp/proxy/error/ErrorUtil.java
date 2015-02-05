package net.lightbody.bmp.proxy.error;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for error-related methods.
 */
public class ErrorUtil {
    private static final Logger log = LoggerFactory.getLogger(ErrorUtil.class);

    private static final String ERROR_HTML_CLASSPATH_LOCATION = "/net/lightbody/bmp/html/error.html";

    /**
     * Minimal HTML for error page if the error.html file cannot be loaded.
     */
    private static final String DEFAULT_ERROR_HTML = "<html><head><title>%s</title></head><body><h1>%s</h1><p>%s</p><p>%s</p></body>";

    /**
     * Lazily-loaded error-page HTML.
     */
    private static volatile String errorHtml;

    /**
     * Returns the error page HTML. The error page HTML contains four String.format-compatible '%s' placeholders for the page title,
     * error title, short description, and long description of the error.
     *
     * @return error page HTML
     */
    public static String getErrorHtml() {
        if (errorHtml == null) {
            loadErrorHtml();
        }

        return errorHtml;
    }

    private static synchronized void loadErrorHtml() {
        if (errorHtml == null) {
            try (InputStream errorHtmlStream = ErrorUtil.class.getResourceAsStream(ERROR_HTML_CLASSPATH_LOCATION)) {
                if (errorHtmlStream != null) {
                    errorHtml = IOUtils.toString(errorHtmlStream);
                } else {
                    log.error("Could not load error.html file. Defaulting to minimalist error page HTML.");
                }
            } catch (IOException e) {
                // classpath resource should always be closeable, so log and ignore
                log.warn("Exception while closing error.html stream", e);

            } catch (RuntimeException e) {
                log.error("Could not load error.html file. Defaulting to minimalist error page HTML.", e);
            }

            if (errorHtml == null) {
                errorHtml = DEFAULT_ERROR_HTML;
            }
        }
    }
}
