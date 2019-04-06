/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.exception;

import com.google.sitebricks.headless.Request;
import com.browserup.bup.filters.JavascriptRequestResponseFilter;

/**
 * Indicates that an error occurred when compiling javascript in {@link JavascriptRequestResponseFilter},
 * for use by {@link com.browserup.bup.proxy.bricks.ProxyResource#addRequestFilter(int, Request)}
 * or {@link com.browserup.bup.proxy.bricks.ProxyResource#addResponseFilter(int, Request)}.
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
