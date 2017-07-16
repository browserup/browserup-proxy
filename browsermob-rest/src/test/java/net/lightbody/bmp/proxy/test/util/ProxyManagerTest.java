package net.lightbody.bmp.proxy.test.util;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.lightbody.bmp.proxy.BrowserMobProxyServerLegacyAdapter;
import net.lightbody.bmp.proxy.ProxyManager;
import net.lightbody.bmp.proxy.guice.ConfigModule;
import org.junit.After;
import org.junit.Before;

public abstract class ProxyManagerTest {
    protected ProxyManager proxyManager;

    public abstract String[] getArgs();

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new ConfigModule(getArgs()));
        proxyManager = injector.getInstance(ProxyManager.class);
    }

    @After
    public void tearDown() throws Exception {
        for(BrowserMobProxyServerLegacyAdapter p : proxyManager.get()){
            try{
                proxyManager.delete(p.getPort());
            }catch(Exception e){ }
        }
    }

}
