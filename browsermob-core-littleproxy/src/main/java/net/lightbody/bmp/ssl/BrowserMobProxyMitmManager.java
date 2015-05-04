package net.lightbody.bmp.ssl;

import org.littleshoot.proxy.MitmManager;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

/**
 * This implementation mirrors the implementation of {@link org.littleshoot.proxy.extras.SelfSignedMitmManager}, but uses the
 * cybervillainsCA.jks keystore that the Jetty implementaion uses.
 */
public class BrowserMobProxyMitmManager implements MitmManager {
    private final BrowserMobSslEngineSource bmpSslEngineSource =
            new BrowserMobSslEngineSource();

    @Override
    public SSLEngine serverSslEngine() {
        return bmpSslEngineSource.newSslEngine();
    }

    @Override
    public SSLEngine clientSslEngineFor(SSLSession serverSslSession) {
        return bmpSslEngineSource.newSslEngine();
    }
}

