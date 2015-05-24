package net.lightbody.bmp.exception;

import com.google.sitebricks.headless.Request;
import net.lightbody.bmp.filters.JavascriptRequestResponseFilter;

/**
 * Indicates that an error occurred when compiling javascript in {@link JavascriptRequestResponseFilter},
 * for use by {@link net.lightbody.bmp.proxy.bricks.ProxyResource#addRequestFilter(int, Request)}
 * or {@link net.lightbody.bmp.proxy.bricks.ProxyResource#addResponseFilter(int, Request)}.
 */
public class JavascriptCompilationException extends RuntimeException {
    public JavascriptCompilationException() {
        super();
    }

    public JavascriptCompilationException(String message) {
        super(message);
    }

    public JavascriptCompilationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JavascriptCompilationException(Throwable cause) {
        super(cause);
    }
}
