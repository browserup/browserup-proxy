package net.lightbody.bmp.proxy;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.test.util.LocalServerTest;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.phantom.resolver.ResolvingPhantomJSDriverService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import static org.junit.Assume.assumeFalse;

public class PhantomJSTest extends LocalServerTest {
    private LegacyProxyServer server;

    @Before
    public void skipForTravisCi() {
        // skipping the phantomjs test on travis ci for now because it sometimes hangs for a few minutes.
        // TODO: fix the cause of the hangs, and improve the phantom js tests to be more useful in general
        assumeFalse("true".equals(System.getenv("TRAVIS")));
    }

    @Before
    public void setUp() throws Exception {
        // start the proxy
        proxy.setCaptureHeaders(true);
        proxy.setCaptureContent(true);
    }
    
    @Test
    public void basicBasic() throws Exception {
        // get the selenium proxy object
        Proxy seleniumProxy = proxy.seleniumProxy();
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

        // ResolvingPhantomJSDriverService downloads PhantomJS if it's not found
		PhantomJSDriver driver = new PhantomJSDriver(
				ResolvingPhantomJSDriverService
						.createDefaultService(capabilities),
				capabilities);
        
        try {
            proxy.newHar("phantomjs-har-test");
    
            driver.get("http://docs.seleniumhq.org");

            Assert.assertThat(driver.getTitle(), CoreMatchers.containsString("Selenium - Web Browser Automation"));
            // get the HAR data
            Har har = proxy.getHar();
    
            // make sure something came back in the har
            Assert.assertFalse(har.getLog().getEntries().isEmpty());
    
            // show that we can capture the HTML of the root page
            String text = har.getLog().getEntries().get(0).getResponse().getContent().getText();
            Assert.assertTrue(text.contains("<title>Selenium - Web Browser Automation</title>"));
        } finally {
            driver.quit();
        }
    }

    @Test
    public void basicSsl() throws Exception {
        // get the selenium proxy object
        Proxy seleniumProxy = proxy.seleniumProxy();
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        capabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[] {"--ignore-ssl-errors=true", "--ssl-protocol=any"});
        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

        // ResolvingPhantomJSDriverService downloads PhantomJS if it's not found
		PhantomJSDriver driver = new PhantomJSDriver(
				ResolvingPhantomJSDriverService
						.createDefaultService(capabilities),
				capabilities);
		
        try {
            proxy.newHar("Google");
    
            // No Country Redirect - always go to the US site
            driver.get("https://www.google.com/ncr");
            Assert.assertThat(driver.getTitle(), CoreMatchers.containsString("Google"));
    
            // get the HAR data
            Har har = proxy.getHar();
    
            // make sure something came back in the har
            Assert.assertFalse(har.getLog().getEntries().isEmpty());
    
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

    @Test
    @Ignore
    public void testPhantomjsLocalServer() throws Exception {
        // get the selenium proxy object
        Proxy seleniumProxy = proxy.seleniumProxy();
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

        // ResolvingPhantomJSDriverService downloads PhantomJS if it's not found
        PhantomJSDriver driver = new PhantomJSDriver(
                ResolvingPhantomJSDriverService
                        .createDefaultService(capabilities),
                capabilities);

        try {
            proxy.newHar("testPhantomjsLocalServer");

            driver.get(getLocalServerHostnameAndPort() + "/echo");

            Assert.assertThat(driver.getPageSource(), CoreMatchers.containsString("Method: GET"));
            // get the HAR data
            Har har = proxy.getHar();

            // make sure something came back in the har
            // TODO: HAR capture is failing with phantomjs and localhost, even though it is working with phantomjs+external servers, and the driver is returning the correct page source
            Assert.assertFalse(har.getLog().getEntries().isEmpty());

            // show that we can capture the HTML of the root page
            String text = har.getLog().getEntries().get(0).getResponse().getContent().getText();
            Assert.assertTrue(text.contains("Method: GET"));
        } finally {
            driver.quit();
        }
    }
}
