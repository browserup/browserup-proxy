package net.lightbody.bmp.proxy.guice;

import com.google.inject.Provider;
import net.lightbody.bmp.proxy.LegacyProxyServer;
import net.lightbody.bmp.proxy.ProxyServer;

public class LegacyProxyServerProvider implements Provider<LegacyProxyServer> {
    @Override
    public LegacyProxyServer get() {
        if (Boolean.getBoolean("bmp.use.littleproxy")) {
            // HACK! since browsermob-core has no knowledge of littleproxy, we have to use reflection to grab the LP implementation
            try {
                Class<LegacyProxyServer> littleProxyImplClass = (Class<LegacyProxyServer>) Class.forName("net.lightbody.bmp.BrowserMobProxyServer");
                LegacyProxyServer littleProxyImpl = littleProxyImplClass.newInstance();

                return littleProxyImpl;
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("The System property bmp.use.littleproxy was true, but the LittleProxy implementation could not be loaded.", e);
            }
        } else {
            return new ProxyServer();
        }
    }
}
