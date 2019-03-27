package com.browserup.bup.util;

public enum HttpStatusClass {
    INFORMATIONAL(100, 200, "Informational"),
    SUCCESS(200, 300, "Success"),
    REDIRECTION(300, 400, "Redirection"),
    CLIENT_ERROR(400, 500, "Client Error"),
    SERVER_ERROR(500, 600, "Server Error"),
    UNKNOWN(0, 0, "Unknown Status") {
        @Override
        public boolean contains(int code) {
            return code < 100 || code >= 600;
        }
    };

    public static HttpStatusClass valueOf(int code) {
        if (INFORMATIONAL.contains(code)) {
            return INFORMATIONAL;
        }
        if (SUCCESS.contains(code)) {
            return SUCCESS;
        }
        if (REDIRECTION.contains(code)) {
            return REDIRECTION;
        }
        if (CLIENT_ERROR.contains(code)) {
            return CLIENT_ERROR;
        }
        if (SERVER_ERROR.contains(code)) {
            return SERVER_ERROR;
        }
        return UNKNOWN;
    }

    private final int min;
    private final int max;
    private final String description;

    HttpStatusClass(int min, int max, String description) {
        this.min = min;
        this.max = max;
        this.description = description;
    }

    public boolean contains(int code) {
        return code >= min && code < max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public String getDescription() {
        return description;
    }
}
