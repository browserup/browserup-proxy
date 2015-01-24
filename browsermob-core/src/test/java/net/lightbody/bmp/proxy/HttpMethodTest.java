package net.lightbody.bmp.proxy;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.junit.After;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class HttpMethodTest {
	private ProxyServer proxyServer;
	protected HttpClient client;
	
	@Before
	public void setUp() {
		proxyServer = new ProxyServer(0);
		proxyServer.start();
		
		client = ProxyServerTest.getNewHttpClient(proxyServer.getPort());
	}
	
	@Test
	public void testPatch() throws ClientProtocolException, IOException {
		// using www.yahoo.com, since it currently seems to respond to a PATCH request the same as a GET.
		HttpResponse response = client.execute(new HttpPatch("https://www.yahoo.com"));

		assertEquals("HTTP PATCH request failed", response.getStatusLine().getStatusCode(), 200);
	}
	
	@After
	public void tearDown() {
		proxyServer.stop();
	}
}
