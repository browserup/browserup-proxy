package net.lightbody.bmp.proxy;

import java.io.IOException;

import net.lightbody.bmp.proxy.test.util.ProxyServerTest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TimeoutsTest {

	private ProxyServer proxyServer;
	private DefaultHttpClient client; 
	
	@Before
	public void setUp() {
		proxyServer = new ProxyServer(0);
		proxyServer.start();
		
		client = ProxyServerTest.getNewHttpClient(proxyServer.getPort());
	}
	
	@Test
	public void testSmallTimeout() throws IllegalStateException, ClientProtocolException, IOException {
		proxyServer.setRequestTimeout(2000);

		HttpGet get = new HttpGet("http://blackhole.webpagetest.org/test");
		
		CloseableHttpResponse response = client.execute(get);
		
		assertEquals("Expected HTTP 502 response due to timeout", 502, response.getStatusLine().getStatusCode());
	}
	
	@After
	public void tearDown() {
		proxyServer.stop();
	}

}
