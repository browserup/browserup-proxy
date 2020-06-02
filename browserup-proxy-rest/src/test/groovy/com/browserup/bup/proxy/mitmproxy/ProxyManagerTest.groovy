/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy.mitmproxy;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.MitmProxyManager;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.guice.ConfigModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;

public abstract class ProxyManagerTest {
    protected MitmProxyManager proxyManager;

    public String[] getArgs() {
        return [] as String[]
    }

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new ConfigModule(getArgs()));
        proxyManager = injector.getInstance(MitmProxyManager.class);
    }

    @After
    public void tearDown() throws Exception {
        for (MitmProxyServer p : proxyManager.get()) {
            try {
                proxyManager.delete(p.getPort());
            } catch (Exception e) {
            }
        }
    }

}
