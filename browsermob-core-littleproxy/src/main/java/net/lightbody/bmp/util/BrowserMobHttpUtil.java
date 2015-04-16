package net.lightbody.bmp.util;

import com.google.common.net.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.exception.DecompressionException;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Utility class with static methods for processing HTTP requests and responses.
 */
public class BrowserMobHttpUtil {
    private static final Logger log = LoggerFactory.getLogger(BrowserMobHttpUtil.class);

    /**
     * Buffer size when decompressing content.
     */
    public static final int DECOMPRESS_BUFFER_SIZE = 16192;

    /**
     * Returns the size of the headers, including the 2 CRLFs at the end of the header block.
     *
     * @param headers headers to size
     * @return length of the headers, in bytes
     */
    public static long getHeaderSize(HttpHeaders headers) {
        long headersSize = 0;
        for (Map.Entry<String, String> header : headers.entries()) {
            // +2 for ': ', +2 for new line
            headersSize += header.getKey().length() + header.getValue().length() + 4;
        }
        return headersSize;
    }

    /**
     * Decompresses the gzipped byte stream.
     *
     * @param fullMessage gzipped byte stream to decomress
     * @return decompressed bytes
     * @throws DecompressionException thrown if the fullMessage cannot be read or decompressed for any reason
     */
    public static byte[] decompressContents(byte[] fullMessage) throws DecompressionException {
        InflaterInputStream gzipReader = null;
        ByteArrayOutputStream uncompressed;
        try {
            gzipReader = new GZIPInputStream(new ByteArrayInputStream(fullMessage));

            uncompressed = new ByteArrayOutputStream(fullMessage.length);

            byte[] decompressBuffer = new byte[DECOMPRESS_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gzipReader.read(decompressBuffer)) > -1) {
                uncompressed.write(decompressBuffer, 0, bytesRead);
            }

            fullMessage = uncompressed.toByteArray();
        } catch (IOException e) {
            throw new DecompressionException("Unable to decompress response", e);
        } finally {
            try {
                if (gzipReader != null) {
                    gzipReader.close();
                }
            } catch (IOException e) {
                log.warn("Unable to close gzip stream", e);
            }
        }
        return fullMessage;
    }

    /**
     * Returns true if the content type string indicates textual content. Currently these are any Content-Types that start with one of the
     * following:
     * <pre>
     *     text/
     *     application/x-javascript
     *     application/javascript
     *     application/json
     *     application/xml
     *     application/xhtml+xml
     * </pre>
     *
     * @param contentType contentType string to parse
     * @return true if the content type is textual
     */
    public static boolean hasTextualContent(String contentType) {
        return contentType != null &&
                (contentType.startsWith("text/") ||
                contentType.startsWith("application/x-javascript") ||
                contentType.startsWith("application/javascript")  ||
                contentType.startsWith("application/json")  ||
                contentType.startsWith("application/xml")  ||
                contentType.startsWith("application/xhtml+xml")
                );
    }

    /**
     * Extracts all readable bytes from the ByteBuf as a byte array.
     *
     * @param content ByteBuf to read
     * @return byte array containing the readable bytes from the ByteBuf
     */
    public static byte[] extractReadableBytes(ByteBuf content) {
        byte[] binaryContent = new byte[content.readableBytes()];

        content.markReaderIndex();
        content.readBytes(binaryContent);
        content.resetReaderIndex();
        return binaryContent;
    }

    /**
     * Converts the byte array into a String based on the charset specified in the contentTypeHeader. If no
     * charset is specified in the contenttypeHeader, this method uses the platform default.
     *
     * @param content bytes to convert to a String
     * @param contentTypeHeader request's content type header
     * @return String containing the converted content
     */
    public static String getContentAsString(byte[] content, String contentTypeHeader) {
        return getContentAsString(content, contentTypeHeader, null);
    }

    /**
     * Converts the byte array into a String based on the charset specified in the contentTypeHeader. If no
     * charset is specified in the contenttypeHeader, this method uses the platform default. The httpRequest is used
     * only for logging purposes if the contentTypeHeader does not contain a charset.
     *
     * @param content bytes to convert to a String
     * @param contentTypeHeader request's content type header
     * @param httpRequest HTTP request responsible for this content (used for logging purposes only)
     * @return String containing the converted content
     */
    public static String getContentAsString(byte[] content, String contentTypeHeader, HttpRequest httpRequest) {
        //FIXME: remove dependency on HttpCore's ContentType
        ContentType contentTypeCharset = ContentType.parse(contentTypeHeader);
        Charset charset = contentTypeCharset.getCharset();
        if (charset == null) {
            // no charset specified, so use the platform-default -- but log a message since this might not encode
            // the data correctly if the browser's default charset is different from this platform's
            charset = Charset.defaultCharset();
            if (httpRequest != null) {
                log.debug("No charset specified; using platform default charset {} to decode contents to/from {}", charset, httpRequest.getUri());
            } else {
                log.debug("No charset specified; using platform default charset {} to decode contents");
            }
        }

        return new String(content, charset);
    }

    /**
     * Identify the host of an HTTP request. This method uses the URI of the request if possible, otherwise it attempts to find the host
     * in the request headers.
     *
     * @param httpRequest HTTP request to parse the host from
     * @return the host the request is connecting to, or null if no host can be found
     */
    public static String identifyHostFromRequest(HttpRequest httpRequest) {
        // use the URI from the request first, if it contains a hostname
        String host = null;
        try {
            URI uri = new URI(httpRequest.getUri());
            host = uri.getHost();
        } catch (URISyntaxException e) {
        }

        // if there was no host in the URI, attempt to grab the host from the HOST header
        if (host == null || host.isEmpty()) {
            // this header parsing logic is taken from ClientToProxyConnection#identifyHostAndPort.
            List<String> hosts = httpRequest.headers().getAll(
                    HttpHeaders.Names.HOST);
            if (hosts != null && !hosts.isEmpty()) {
                String hostAndPort = hosts.get(0);
                HostAndPort parsedHostAndPort = HostAndPort.fromString(hostAndPort);

                host = parsedHostAndPort.getHostText();
            }
        }

        return host;
    }
}
