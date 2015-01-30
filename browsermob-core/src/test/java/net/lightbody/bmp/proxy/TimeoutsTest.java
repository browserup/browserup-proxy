package net.lightbody.bmp.proxy;

import net.lightbody.bmp.proxy.test.util.ProxyServerTest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TimeoutsTest extends ProxyServerTest {
	@Test
	public void testSmallTimeout() throws IllegalStateException, IOException {
		proxy.setRequestTimeout(2000);

		HttpGet get = new HttpGet("http://blackhole.webpagetest.org/test");
		
		CloseableHttpResponse response = client.execute(get);
		
		assertEquals("Expected HTTP 502 response due to timeout", 502, response.getStatusLine().getStatusCode());
	}

}
