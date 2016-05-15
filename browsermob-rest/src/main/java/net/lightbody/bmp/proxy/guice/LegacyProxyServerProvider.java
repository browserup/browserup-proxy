package net.lightbody.bmp.proxy.guice;

import com.google.inject.Provider;
import net.lightbody.bmp.BrowserMobProxyServerLegacyAdapter;
import net.lightbody.bmp.proxy.LegacyProxyServer;
import net.lightbody.bmp.proxy.ProxyServer;

public class LegacyProxyServerProvider implements Provider<LegacyProxyServer> {
    // temporary, until REST API is replaced
    public static volatile boolean useLittleProxy = false;

    @Override
    public LegacyProxyServer get() {
        if (useLittleProxy || Boolean.getBoolean("bmp.use.littleproxy")) {
            return new BrowserMobProxyServerLegacyAdapter();
        } else {
            return new ProxyServer();
        }
    }
}
