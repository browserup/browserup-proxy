package net.lightbody.bmp.proxy;

import net.lightbody.bmp.proxy.test.util.LocalServerTest;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpMethodTest extends LocalServerTest {

	@Test
	public void testPatch() throws IOException {
		HttpResponse response = client.execute(new HttpPatch(getLocalServerHostnameAndPort() + "/echo"));
		String body = IOUtils.toStringAndClose(response.getEntity().getContent());

		assertEquals("HTTP PATCH request failed", 200, response.getStatusLine().getStatusCode());
		assertTrue("Server reported HTTP method was not PATCH. Body was: " + body, body.contains("Method: PATCH"));
	}

	@Test
	public void testOptions() throws IOException {
		HttpResponse response = client.execute(new HttpOptions(getLocalServerHostnameAndPort() + "/echo"));
		String body = IOUtils.toStringAndClose(response.getEntity().getContent());

		assertEquals("HTTP OPTIONS request failed", 200, response.getStatusLine().getStatusCode());
		assertTrue("Server reported HTTP method was not OPTIONS. Body was: " + body, body.contains("Method: OPTIONS"));
	}

	@Test
	public void testHead() throws IOException {
		HttpResponse response = client.execute(new HttpHead(getLocalServerHostnameAndPort() + "/echo"));
		// HEAD responses don't contain entities

		assertEquals("HTTP HEAD request failed", 200, response.getStatusLine().getStatusCode());
	}

	@Test
	public void testDelete() throws IOException {
		HttpResponse response = client.execute(new HttpDelete(getLocalServerHostnameAndPort() + "/echo"));
		String body = IOUtils.toStringAndClose(response.getEntity().getContent());

		assertEquals("HTTP DELETE request failed", 200, response.getStatusLine().getStatusCode());
		assertTrue("Server reported HTTP method was not DELETE. Body was: " + body, body.contains("Method: DELETE"));
	}

	@Test
	public void testPut() throws IOException {
		HttpResponse response = client.execute(new HttpPut(getLocalServerHostnameAndPort() + "/echo"));
		String body = IOUtils.toStringAndClose(response.getEntity().getContent());

		assertEquals("HTTP PUT request failed", 200, response.getStatusLine().getStatusCode());
		assertTrue("Server reported HTTP method was not PUT. Body was: " + body, body.contains("Method: PUT"));
	}

	@Test
	public void testTrace() throws IOException {
		HttpResponse response = client.execute(new HttpTrace(getLocalServerHostnameAndPort() + "/echo"));
		String body = IOUtils.toStringAndClose(response.getEntity().getContent());

		assertEquals("HTTP TRACE request failed", 200, response.getStatusLine().getStatusCode());
		assertTrue("Server reported HTTP method was not TRACE. Body was: " + body, body.contains("Method: TRACE"));
	}
	
}
