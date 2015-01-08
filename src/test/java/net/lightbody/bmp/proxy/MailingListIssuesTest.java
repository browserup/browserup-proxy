package net.lightbody.bmp.proxy;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarPostData;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.util.ThreadUtils;
import net.lightbody.bmp.proxy.http.BrowserMobHttpRequest;
import net.lightbody.bmp.proxy.http.BrowserMobHttpResponse;
import net.lightbody.bmp.proxy.http.RequestInterceptor;
import net.lightbody.bmp.proxy.http.ResponseInterceptor;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MailingListIssuesTest extends DummyServerTest {
    @Test
    public void testThatInterceptorIsCalled() throws IOException, InterruptedException {
        final boolean[] interceptorHit = {false};
        proxy.addRequestInterceptor(new RequestInterceptor() {
            @Override
            public void process(BrowserMobHttpRequest request, Har har) {
                interceptorHit[0] = true;
            }
        });

        String body = IOUtils.readFully(client.execute(new HttpGet("http://127.0.0.1:8080/a.txt")).getEntity().getContent());

        Assert.assertTrue(body.contains("this is a.txt"));
        Assert.assertTrue(interceptorHit[0]);
    }

    @Test
    public void testThatInterceptorCanCaptureCallingIpAddress() throws IOException, InterruptedException {
        final String[] remoteHost = {null};
        proxy.addRequestInterceptor(new RequestInterceptor() {
            @Override
            public void process(BrowserMobHttpRequest request, Har har) {
                remoteHost[0] = request.getProxyRequest().getRemoteHost();
            }
        });

        String body = IOUtils.readFully(client.execute(new HttpGet("http://127.0.0.1:8080/a.txt")).getEntity().getContent());

        Assert.assertTrue(body.contains("this is a.txt"));
        Assert.assertEquals("Remote host incorrect", "127.0.0.1", remoteHost[0]);
    }

    @Test
    public void testThatWeCanChangeTheUserAgent() throws IOException, InterruptedException {
        proxy.addRequestInterceptor(new RequestInterceptor() {
            @Override
            public void process(BrowserMobHttpRequest request, Har har) {
                request.getMethod().removeHeaders("User-Agent");
                request.getMethod().addHeader("User-Agent", "Bananabot/1.0");
            }
        });

        String body = IOUtils.readFully(client.execute(new HttpGet("http://127.0.0.1:8080/a.txt")).getEntity().getContent());

        Assert.assertTrue(body.contains("this is a.txt"));
    }

    @Test
    public void testThatInterceptorsCanRewriteUrls() throws IOException, InterruptedException {
        proxy.addRequestInterceptor(new RequestInterceptor() {
            @Override
            public void process(BrowserMobHttpRequest request, Har har) {
                try {
                    request.getMethod().setURI(new URI("http://127.0.0.1:8080/b.txt"));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });

        String body = IOUtils.readFully(client.execute(new HttpGet("http://127.0.0.1:8080/a.txt")).getEntity().getContent());

        Assert.assertTrue(body.contains("this is b.txt"));
    }

    @Test
    public void testThatInterceptorsCanReadResponseBodies() throws IOException, InterruptedException {
        final String[] interceptedBody = {null};

        proxy.setCaptureContent(true);
        proxy.addResponseInterceptor(new ResponseInterceptor() {
            @Override
            public void process(BrowserMobHttpResponse response, Har har) {
                interceptedBody[0] = response.getEntry().getResponse().getContent().getText();
            }
        });

        String body = IOUtils.readFully(client.execute(new HttpGet("http://127.0.0.1:8080/a.txt")).getEntity().getContent());

        ThreadUtils.waitFor(new ThreadUtils.WaitCondition() {
            @Override
            public boolean checkCondition(long elapsedTimeInMs) {
                return interceptedBody[0] != null;
            }
        }, TimeUnit.SECONDS, 10);

        Assert.assertEquals(interceptedBody[0], body);
    }

    @Test
    @Ignore
    public void testThatInterceptorsCanReadPostParamaters() throws IOException, InterruptedException {

        proxy.setCaptureContent(true);
        proxy.newHar("test");

        final String[] capturedPostData = new String[2];

        proxy.addRequestInterceptor(new RequestInterceptor() {
            @Override
            public void process(BrowserMobHttpRequest request, Har har) {
                capturedPostData[0] = request.getProxyRequest().getParameter("testParam");
            }
        });

        HttpPost post = new HttpPost("http://127.0.0.1:8080/echo/");
        HttpEntity entity = new StringEntity("testParam=testValue");
        post.setEntity(entity);
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");

        client.execute(post);

        Har har = proxy.getHar();
        HarLog log = har.getLog();
        List<HarEntry> entries = log.getEntries();
        HarEntry entry = entries.get(0);
        HarRequest request = entry.getRequest();
        HarPostData postdata = request.getPostData();
        capturedPostData[1] = postdata.getParams().get(0).getValue();

        System.out.println(capturedPostData[0]);
        System.out.println(capturedPostData[1]);

        boolean postDataCapturedAndLoggedCorrectly = capturedPostData[0].equals(capturedPostData[1]);

        Assert.assertEquals(true,postDataCapturedAndLoggedCorrectly);
    }

    @Test
    public void issue27() throws Exception{
        // see: https://github.com/lightbody/browsermob-proxy/issues/27
        WebDriver driver = null;
        // start the proxy
        ProxyServer server = new ProxyServer(4444);
        server.start();
        try {
            server.setCaptureHeaders(true);
            server.setCaptureContent(true);
    
            // get the selenium proxy object
            Proxy proxy = server.seleniumProxy();
            DesiredCapabilities capabilities = new DesiredCapabilities();
    
            capabilities.setCapability(CapabilityType.PROXY, proxy);
    
            // start the browser up
            driver = new FirefoxDriver(capabilities);
    
            server.newHar("assertselenium.com");
    
            driver.get("http://whatsmyuseragent.com");
            //driver.get("https://google.com");
    
            // get the HAR data
            Har har = server.getHar();
    
            // make sure something came back in the har
            Assert.assertTrue(!har.getLog().getEntries().isEmpty());
    
            // show that we can capture the HTML of the root page
            String text = har.getLog().getEntries().get(0).getResponse().getContent().getText();
            Assert.assertTrue(text.contains("My User Agent?"));
        } finally {
            server.stop();
            if (driver != null) {
                driver.quit();
            }
        }
    }

    @Test
    public void googleCaSslNotWorkingInFirefox() throws Exception{
        WebDriver driver = null;
        // start the proxy
        ProxyServer server = new ProxyServer(4444);
        server.start();
        try {
            server.setCaptureHeaders(true);
            server.setCaptureContent(true);
    
            // get the selenium proxy object
            Proxy proxy = server.seleniumProxy();
            DesiredCapabilities capabilities = new DesiredCapabilities();
    
            capabilities.setCapability(CapabilityType.PROXY, proxy);
    
            // start the browser up
            driver = new FirefoxDriver(capabilities);
    
            server.newHar("Google.ca");
    
            driver.get("https://www.google.ca/");
    
            // get the HAR data
            Har har = server.getHar();
    
            // make sure something came back in the har
            Assert.assertTrue(!har.getLog().getEntries().isEmpty());
    
            // show that we can capture the HTML of the root page
            String text = har.getLog().getEntries().get(0).getResponse().getContent().getText();
            Assert.assertTrue(text.contains("<title>Google</title>"));
        } finally {
            server.stop();
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
	@Test
	public void testProxyConfigurationThroughFirefoxProfile() {
		ProxyServer server = new ProxyServer(0);
		server.start();
		
		int port = server.getPort();
		
		WebDriver driver = null;

		try {
			FirefoxProfile profile = new FirefoxProfile();
			profile.setAcceptUntrustedCertificates(true);
			profile.setAssumeUntrustedCertificateIssuer(true);
			profile.setPreference("network.proxy.http", "localhost");
			profile.setPreference("network.proxy.http_port", port);
			profile.setPreference("network.proxy.ssl", "localhost");
			profile.setPreference("network.proxy.ssl_port", port);
			profile.setPreference("network.proxy.type", 1);
			profile.setPreference("network.proxy.no_proxies_on", "");

			DesiredCapabilities capabilities = new DesiredCapabilities();

			capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
			capabilities.setCapability(FirefoxDriver.PROFILE, profile);
			capabilities.setCapability(CapabilityType.PROXY,
					server.seleniumProxy());

			driver = new FirefoxDriver(capabilities);
			driver.get("https://www.gmail.com/");
		} finally {
			server.stop();
			
			if (driver != null) {
				driver.close();
			}
		}
	}

}
