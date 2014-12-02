package net.lightbody.bmp.proxy;

import java.util.List;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarNameVersion;

import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class HarResultsTest extends DummyServerTest {
    @Test
    public void testRequestAndResponseSizesAreSet() throws Exception {

        // see https://github.com/lightbody/browsermob-proxy/pull/36 for history

        proxy.setCaptureContent(true);
        proxy.newHar("test");

        HttpGet get = new HttpGet("http://127.0.0.1:8080/a.txt");
        client.execute(get);

        Har har = proxy.getHar();
        HarLog log = har.getLog();
        List<HarEntry> entries = log.getEntries();
        HarEntry entry = entries.get(0);

        /*
        Request headers should be something like this:

        Host: 127.0.0.1:8080
        User-Agent: bmp.lightbody.net/2.0-beta-10-SNAPSHOT
         */
        Assert.assertTrue("Minimum header size not seen", entry.getRequest().getHeadersSize() > 70);
        Assert.assertEquals(0, entry.getRequest().getBodySize());

        /*
        Response headers should be something like this:

        Date: Sun, 31 Aug 2014 16:08:44 GMT
        Server: Jetty/5.1.x (Mac OS X/10.9.4 x86_64 java/1.7.0_09
        Content-Type: text/plain
        Content-Length: 13
        Last-Modified: Sun, 17 Nov 2013 05:37:58 GMT
        Accept-Ranges: bytes
         */
        Assert.assertTrue("Minimum header size not seen", entry.getResponse().getHeadersSize() > 200);
        Assert.assertEquals(13, entry.getResponse().getBodySize());
    }
    
	@Test
	public void testHarContainsUserAgent() {
		ProxyServer server = new ProxyServer(0);
		server.start();
		
		WebDriver driver = null;
		try {
			server.setCaptureHeaders(true);
			server.newHar("testHarContainsUserAgent");
			
			Proxy proxy = server.seleniumProxy();
			DesiredCapabilities capabilities = new DesiredCapabilities();
			
			capabilities.setCapability(CapabilityType.PROXY, proxy);
			
			driver = new FirefoxDriver(capabilities);
			
			driver.get("https://www.google.com");
			
			Har har = server.getHar();
			Assert.assertNotNull("Har is null", har);
			HarLog log = har.getLog();
			Assert.assertNotNull("Log is null", log);
			HarNameVersion harNameVersion = log.getBrowser();
			Assert.assertNotNull("HarNameVersion is null", harNameVersion);
			
			Assert.assertEquals("Expected browser to be Firefox", "Firefox", harNameVersion.getName());
			Assert.assertNotNull("browser version is null", harNameVersion.getVersion());
		} finally {
			server.stop();
			if (driver != null) {
				driver.quit();
			}
		}
	}
}
