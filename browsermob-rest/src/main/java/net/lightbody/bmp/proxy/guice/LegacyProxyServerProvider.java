package net.lightbody.bmp.proxy.guice;

import com.google.inject.Provider;
import net.lightbody.bmp.proxy.LegacyProxyServer;
import net.lightbody.bmp.proxy.ProxyServer;

public class LegacyProxyServerProvider implements Provider<LegacyProxyServer> {
    @Override
    public LegacyProxyServer get() {
        return new ProxyServer();
    }
}