package net.lightbody.bmp.proxy.http;

import net.lightbody.bmp.core.har.Har;

public interface RequestInterceptor {
    void process(BrowserMobHttpRequest request, Har har);
}
