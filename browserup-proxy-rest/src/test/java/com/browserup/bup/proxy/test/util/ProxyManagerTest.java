package com.browserup.bup.proxy.test.util;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.guice.ConfigModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class ProxyManagerTest {
    protected ProxyManager proxyManager;

    public String[] getArgs() {
        return new String[] {};
    }

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new ConfigModule(getArgs()));
        proxyManager = injector.getInstance(ProxyManager.class);
    }

    @After
    public void tearDown() throws Exception {
        for(BrowserUpProxyServer p : proxyManager.get()){
            try{
                proxyManager.delete(p.getPort());
            }catch(Exception e){ }
        }
    }
}
