package net.lightbody.bmp.proxy.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class IOUtils {
    /**
     * Copies the input stream to the output stream and closes both streams. Both streams are guaranteed to be closed, even if the copy
     * operation throws an exception. The copy operation may throw IOException, but closing either stream will not throw IOException.
     *
     * @param in InputStream to read and close
     * @param out OutputStream to read and close
     * @throws IOException if an error occurs reading or writing to/from the streams
     */
    public static void copyAndClose(InputStream in, OutputStream out) throws IOException {
        try {
            org.apache.commons.io.IOUtils.copy(in, out);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(in);
            org.apache.commons.io.IOUtils.closeQuietly(out);
        }
    }

    /**
     * Reads and closes the input stream, converting it to a String using the UTF-8 charset. The input stream is guaranteed to be closed, even
     * if the reading/conversion throws an exception.
     *
     * @param in UTF-8-encoded InputStream to read
     * @return String of InputStream's contents
     * @throws IOException if an error occurs reading from the stream
     */
    public static String toStringAndClose(InputStream in) throws IOException {
        try {
            return org.apache.commons.io.IOUtils.toString(in, StandardCharsets.UTF_8);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(in);
        }
    }
}
