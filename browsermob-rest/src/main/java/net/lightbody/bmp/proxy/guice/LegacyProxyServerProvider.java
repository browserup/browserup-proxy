package net.lightbody.bmp.proxy.guice;

import com.google.inject.Provider;
import net.lightbody.bmp.proxy.BrowserMobProxyServerLegacyAdapter;

public class LegacyProxyServerProvider implements Provider<BrowserMobProxyServerLegacyAdapter> {
    @Override
    public BrowserMobProxyServerLegacyAdapter get() {
        return new BrowserMobProxyServerLegacyAdapter();
    }
}
