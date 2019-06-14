package com.browserup.bup.proxy.hk;

import com.browserup.bup.proxy.ProxyManager;
import org.glassfish.hk2.api.Factory;

public class ProxyManagerFactory implements Factory<ProxyManager> {
    @Override
    public ProxyManager provide() {
        return null;
    }

    @Override
    public void dispose(ProxyManager instance) {

    }
}
