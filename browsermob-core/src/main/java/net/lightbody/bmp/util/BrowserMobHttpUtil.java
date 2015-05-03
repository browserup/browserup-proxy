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
import java.nio.charset.StandardCharsets;
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
     * Default MIME content type if no Content-Type header is present. According to the HTTP 1.1 spec, section 7.2.1:
     * <pre>
     *     Any HTTP/1.1 message containing an entity-body SHOULD include a Content-Type header field defining the media
     *     type of that body. If and only if the media type is not given by a Content-Type field, the recipient MAY
     *     attempt to guess the media type via inspection of its content and/or the name extension(s) of the URI used to
     *     identify the resource. If the media type remains unknown, the recipient SHOULD treat it as
     *     type "application/octet-stream".
     * </pre>
     */
    public static final String UNKNOWN_CONTENT_TYPE = "application/octet-stream";

    /**
     * The default charset when the Content-Type header does not specify a charset. From the HTTP 1.1 spec section 3.7.1:
     * <pre>
     *     When no explicit charset parameter is provided by the sender, media subtypes of the "text" type are defined to have a default
     *     charset value of "ISO-8859-1" when received via HTTP. Data in character sets other than "ISO-8859-1" or its subsets MUST be
     *     labeled with an appropriate charset value.
     * </pre>
     */
    public static final Charset DEFAULT_HTTP_CHARSET = StandardCharsets.ISO_8859_1;

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
     * charset is specified in the contentTypeHeader, this method uses default (see {@link #DEFAULT_HTTP_CHARSET}). The httpRequest is used
     * only for logging purposes if the contentTypeHeader does not contain a charset.
     *
     * @param content bytes to convert to a String
     * @param contentTypeHeader request's content type header
     * @param httpRequest HTTP request responsible for this content (used for logging purposes only)
     * @return String containing the converted content
     */
    public static String getContentAsString(byte[] content, String contentTypeHeader, HttpRequest httpRequest) {
        Charset charset = readCharsetInContentTypeHeader(contentTypeHeader);
        if (charset == null) {
            // no charset specified, so use the default -- but log a message since this might not encode the data correctly
            charset = DEFAULT_HTTP_CHARSET;
            if (httpRequest != null) {
                log.debug("No charset specified; using charset {} to decode contents to/from {}", charset, httpRequest.getUri());
            } else {
                log.debug("No charset specified; using charset {} to decode contents", charset);
            }
        }

        return new String(content, charset);
    }

    /**
     * Derives the charset from the Content-Type header. Unlike {@link #readCharsetInContentTypeHeader}, if contentTypeHeader is null or
     * does not specify a charset, this method will return the ISO-8859-1 charset.
     *
     * @param contentTypeHeader the Content-Type header string; can be null or empty
     * @return the character set indicated in the contentTypeHeader, or ISO-8859-1 if none is specified or no contentTypeHeader is specified
     */
    public static Charset deriveCharsetFromContentTypeHeader(String contentTypeHeader) {
        Charset charset = readCharsetInContentTypeHeader(contentTypeHeader);
        if (charset == null) {
            return DEFAULT_HTTP_CHARSET;
        }

        return charset;
    }

    /**
     * Reads the charset directly from the Content-Type header string. If the Content-Type header does not contain a charset, or if the header
     * is null or empty, this method returns null. See also {@link #deriveCharsetFromContentTypeHeader(String)}.
     *
     * @param contentTypeHeader the Content-Type header string; can be null or empty
     * @return the character set indicated in the contentTypeHeader, or null if the charset is not present
     */
    public static Charset readCharsetInContentTypeHeader(String contentTypeHeader) {
        if (contentTypeHeader == null || contentTypeHeader.isEmpty()) {
            return DEFAULT_HTTP_CHARSET;
        }

        //FIXME: remove dependency on HttpCore's ContentType
        ContentType contentTypeCharset = ContentType.parse(contentTypeHeader);

        return contentTypeCharset.getCharset();
    }

    /**
     * Identify the host of an HTTP request. This method uses the URI of the request if possible, otherwise it attempts to find the host
     * in the request headers.
     *
     * @param httpRequest HTTP request to parse the host from
     * @return the host the request is connecting to, or null if no host can be found
     */
    public static String identifyHostFromRequest(HttpRequest httpRequest) {
        // try to use the URI from the request first, if the URI starts with http:// or https://. checking for http/https avoids confusing
        // java's URI class when the request is for a malformed URL like '//some-resource'.
        String host = null;
        if (startsWithHttpOrHttps(httpRequest.getUri())) {
            try {
                URI uri = new URI(httpRequest.getUri());
                host = uri.getHost();
            } catch (URISyntaxException e) {
            }
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

    /**
     * Returns true if the string starts with http:// or https://.
     *
     * @param uri string to evaluate
     * @return true if the string starts with http:// or https://
     */
    public static boolean startsWithHttpOrHttps(String uri) {
        if (uri == null) {
            return false;
        }

        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            return true;
        } else {
            return false;
        }
    }
}
