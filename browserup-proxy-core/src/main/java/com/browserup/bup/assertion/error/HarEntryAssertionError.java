package com.browserup.bup.assertion.error;

public class HarEntryAssertionError {
    private final String message;

    public HarEntryAssertionError(String message) {
        this.message = message;
    }

    public HarEntryAssertionError(Object expected, Object actual) {
        this.message = String.format("Assertion failed, expected: %s, actual: %s",
                String.valueOf(expected), String.valueOf(actual));
    }

    public HarEntryAssertionError(String prefixMessage, Object expected, Object actual) {
        this.message = String.format("%s. Expected: %s, actual: %s",
                prefixMessage, String.valueOf(expected), String.valueOf(actual));
    }

    public String getMessage() {
        return message;
    }
}
