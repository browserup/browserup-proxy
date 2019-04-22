package com.browserup.harreader;

public enum HarReaderMode {

    /**
     * Using strict mode enforces some rules.
     * When trying to open an invalid HAR file an exception will be thrown.
     */
    STRICT,

    /**
     * Using lax mode you are able to read even invalid HAR files.
     * Currently lax mode allows:
     * <ul>
     *     <li>invalid date formats</li>
     * </ul>
     */
    LAX;
}
