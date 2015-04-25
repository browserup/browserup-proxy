package net.lightbody.bmp.util;

import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;

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
    public static void replaceTextHttpEntityBody(FullHttpMessage message, String newContents) {
        // get the content type for this message so we can encode the newContents into a byte stream appropriately
        String contentTypeHeader = message.headers().get(HttpHeaders.Names.CONTENT_TYPE);
        Charset messageCharset = BrowserMobHttpUtil.deriveCharsetFromContentTypeHeader(contentTypeHeader);

        byte[] contentBytes = newContents.getBytes(messageCharset);

        replaceBinaryHttpEntityBody(message, contentBytes);
    }

    /**
     * Replaces an HTTP entity body with the specified binary contents.
     * TODO: Currently this method only works for FullHttpMessages, since it must modify the Content-Length header; determine if this may be applied to chunked messages as well
     *
     * @param message the HTTP message to manipulate
     * @param newBinaryContents the new entity body contents
     */
    public static void replaceBinaryHttpEntityBody(FullHttpMessage message, byte[] newBinaryContents) {
        message.content().resetWriterIndex();
        // resize the buffer if needed, since the new message may be longer than the old one
        message.content().ensureWritable(newBinaryContents.length, true);
        message.content().writeBytes(newBinaryContents);

        // update the Content-Length header, since the size may have changed
        message.headers().set(HttpHeaders.Names.CONTENT_LENGTH, newBinaryContents.length);
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
        Charset charset = getCharsetFromMessage(httpMessage);

        return extractHttpEntityBody(httpMessage, charset);
    }

    /**
     * Derives the charset from the Content-Type header in the HttpMessage. If the Content-Type header is not present or does not contain
     * a character set, this method returns the ISO-8859-1 character set. See {@link BrowserMobHttpUtil#deriveCharsetFromContentTypeHeader(String)}
     * for more details.
     *
     * @param httpMessage HTTP message to extract charset from
     * @return the charset assocaited with the HTTP message, or the default charset if none is present
     */
    public static Charset getCharsetFromMessage(HttpMessage httpMessage) {
        String contentTypeHeader = HttpHeaders.getHeader(httpMessage, HttpHeaders.Names.CONTENT_TYPE);
        return BrowserMobHttpUtil.deriveCharsetFromContentTypeHeader(contentTypeHeader);
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
