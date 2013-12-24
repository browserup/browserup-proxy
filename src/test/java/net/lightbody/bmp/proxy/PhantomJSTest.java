package net.lightbody.bmp.proxy;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class PhantomJSTest {
    
    ProxyServer server;
    
    @Before
    public void startProxy() throws Exception {
        // start the proxy
        server = new ProxyServer(4444);
        server.start();
        server.setCaptureHeaders(true);
        server.setCaptureContent(true);
    }
    
    @After
    public void stopProxy() throws Exception {
        // always stop the proxy after each test, even if test failed.
        if (server != null) {
            server.stop();
            server = null;
        }
    }
    
    @Test
    public void basicBasic() throws Exception {
        // get the selenium proxy object
        Proxy proxy = server.seleniumProxy();
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability(CapabilityType.PROXY, proxy);

        PhantomJSDriver driver = new PhantomJSDriver(capabilities);
        
        try {
            server.newHar("Yahoo");
    
            driver.get("http://us.yahoo.com");
    
            Assert.assertThat(driver.getTitle(), CoreMatchers.containsString("Yahoo"));
            // get the HAR data
            Har har = server.getHar();
    
            // make sure something came back in the har
            Assert.assertTrue(!har.getLog().getEntries().isEmpty());
    
            // show that we can capture the HTML of the root page
            String text = har.getLog().getEntries().get(0).getResponse().getContent().getText();
            Assert.assertTrue(text.contains("<title>Yahoo</title>"));
        } finally {
            driver.quit();
        }

    }

    @Test
    public void basicSsl() throws Exception {
        // get the selenium proxy object
        Proxy proxy = server.seleniumProxy();
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        capabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[] {"--ignore-ssl-errors=true", "--ssl-protocol=any"});
        capabilities.setCapability(CapabilityType.PROXY, proxy);

        PhantomJSDriver driver = new PhantomJSDriver(capabilities);
        try {
            server.newHar("Google");
    
            // No Country Redirect - always go to the US site
            driver.get("https://www.google.com/ncr");
            Assert.assertThat(driver.getTitle(), CoreMatchers.containsString("Google"));
    
            // get the HAR data
            Har har = server.getHar();
    
            // make sure something came back in the har
            Assert.assertTrue(!har.getLog().getEntries().isEmpty());
    
            // show that we can capture the HTML of the root page
            String text = null;
            for (HarEntry entry : har.getLog().getEntries()) {
                // find the first proper response, and check it
                if (entry.getResponse().getStatus() == 200) {
                    text = entry.getResponse().getContent().getText();
                    Assert.assertTrue(text.contains("<title>Google</title>"));
                    // nothing left to prove
                    return;
                }
                
            }
            Assert.fail("No normal (Status 200) response found in HAR");
        } finally {
            driver.quit();
        }

    }
}
