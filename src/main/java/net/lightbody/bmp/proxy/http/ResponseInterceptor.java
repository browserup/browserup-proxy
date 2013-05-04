package net.lightbody.bmp.proxy.http;

public interface ResponseInterceptor {
    void process(BrowserMobHttpResponse response);
}
