/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.exception;

/**
 * Indicates that an error occurred while decompressing content.
 */
public class DecompressionException extends RuntimeException {
    private static final long serialVersionUID = 8666473793514307564L;

    public DecompressionException() {
    }

    public DecompressionException(String message) {
        super(message);
    }

    public DecompressionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecompressionException(Throwable cause) {
        super(cause);
    }
}
