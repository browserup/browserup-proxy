package net.lightbody.bmp.proxy;

import java.io.IOException;

import net.lightbody.bmp.proxy.test.util.ProxyServerTest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.util.EntityUtils;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

public class HttpMethodTest extends ProxyServerTest {

	@Test
	@Ignore
	//TODO: Replace this external call with a call to an internal servlet that will echo the HTTP method in the body, so we can
	//verify that the proxy handled the PATCH correctly
	public void testPatch() throws IOException {
		// using www.yahoo.com, since it currently seems to respond to a PATCH request the same as a GET. [note: this is unstable/unreliable]
		HttpResponse response = client.execute(new HttpPatch("http://www.yahoo.com"));
		EntityUtils.consumeQuietly(response.getEntity());

		assertEquals("HTTP PATCH request failed", 200, response.getStatusLine().getStatusCode());
	}
	
}
