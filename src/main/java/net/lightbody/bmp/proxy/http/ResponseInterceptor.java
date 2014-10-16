package net.lightbody.bmp.proxy.http;

import net.lightbody.bmp.core.har.Har;

public interface ResponseInterceptor {
    void process(BrowserMobHttpResponse response, Har har);
}
