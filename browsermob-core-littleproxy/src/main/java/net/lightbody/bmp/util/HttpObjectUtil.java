package net.lightbody.bmp.util;

import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;

import java.nio.charset.Charset;

/**
 * Utility class to assist with manipulation of {@link io.netty.handler.codec.http.HttpObject} instances, including
 * {@link io.netty.handler.codec.http.HttpMessage} and {@link io.netty.handler.codec.http.HttpContent}.
 */
public class HttpObjectUtil {

    /**
     * Replaces the entity body of the message with the specified contents. Encodes the message contents according to charset in the message's
     * Content-Type header, or uses {@link BrowserMobHttpUtil#DEFAULT_HTTP_CHARSET} if none is specified.
     * TODO: Currently this method only works for FullHttpMessages, since it must modify the Content-Length header; determine if this may be applied to chunked messages as well
     *
     * @param message the HTTP message to manipulate
     * @param newContents the new entity body contents
     */
    public static void replaceHttpEntityBody(FullHttpMessage message, String newContents) {
        // get the content type for this message so we can encode the newContents into a byte stream appropriately
        String contentTypeHeader = message.headers().get(HttpHeaders.Names.CONTENT_TYPE);
        Charset messageCharset = BrowserMobHttpUtil.deriveCharsetFromContentTypeHeader(contentTypeHeader);

        byte[] contentBytes = newContents.getBytes(messageCharset);

        message.content().resetWriterIndex();
        // resize the buffer if needed, since the new message may be longer than the old one
        message.content().ensureWritable(contentBytes.length, true);
        message.content().writeBytes(contentBytes);

        // update the Content-Length header, since the size may have changed
        message.headers().set(HttpHeaders.Names.CONTENT_LENGTH, contentBytes.length);
    }

    /**
     * Extracts the entity body from an HTTP content object, according to the specified character set. The character set cannot be null. If
     * the character set is not specified or is unknown, you still must specify a suitable default charset (see {@link BrowserMobHttpUtil#DEFAULT_HTTP_CHARSET}).
     *
     * @param httpContent HTTP content object to extract the entity body from
     * @param charset character set of the entity body
     * @return String representation of the entity body
     * @throws IllegalArgumentException if the charset is null
     */
    public static String extractHttpEntityBody(HttpContent httpContent, Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("No charset specified when extracting the contents of an HTTP message");
        }

        byte[] contentBytes = BrowserMobHttpUtil.extractReadableBytes(httpContent.content());

        return new String(contentBytes, charset);
    }

    /**
     * Extracts the entity body from a FullHttpMessage, according to the character set in the message's Content-Type header. If the Content-Type
     * header is not present or does not specify a charset, assumes the ISO-8859-1 character set (see {@link BrowserMobHttpUtil#DEFAULT_HTTP_CHARSET}).
     *
     * @param httpMessage HTTP message to extract entity body from
     * @return String representation of the entity body
     */
    public static String extractHttpEntityBody(FullHttpMessage httpMessage) {
        String contentTypeHeader = HttpHeaders.getHeader(httpMessage, HttpHeaders.Names.CONTENT_TYPE);
        Charset charset = BrowserMobHttpUtil.deriveCharsetFromContentTypeHeader(contentTypeHeader);

        return extractHttpEntityBody(httpMessage, charset);
    }

    /**
     * Extracts the binary contents from an HTTP message.
     *
     * @param httpContent HTTP content object to extract the entity body from
     * @return binary contents of the HTTP message
     */
    public static byte[] extractBinaryHttpEntityBody(HttpContent httpContent) {
        return BrowserMobHttpUtil.extractReadableBytes(httpContent.content());
    }
}
