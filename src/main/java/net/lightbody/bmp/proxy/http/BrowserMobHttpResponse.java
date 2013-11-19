package net.lightbody.bmp.proxy.http;

import net.lightbody.bmp.core.har.HarEntry;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

public class BrowserMobHttpResponse {
    private HarEntry entry;
    private HttpRequestBase method;
    private HttpResponse response;
    private String errorMessage;
    private String contentType;
    private String charSet;

    public BrowserMobHttpResponse(HarEntry entry, HttpRequestBase method, HttpResponse response, String errorMessage, String contentType, String charSet) {
        this.entry = entry;
        this.method = method;
        this.response = response;
        this.errorMessage = errorMessage;
        this.contentType = contentType;
        this.charSet = charSet;
    }

    public String getContentType() {
        return contentType;
    }

    public String getCharSet() {
        return charSet;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getHeader(String name) {
        Header header = response.getFirstHeader(name);
        if (header == null) {
            return null;
        }

        return header.getValue();
    }

    public HttpResponse getRawResponse() {
        return response;
    }

    public HarEntry getEntry() {
        return entry;
    }
}
