package net.lightbody.bmp.proxy;

import net.lightbody.bmp.proxy.test.util.LocalServerTest;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class AddHeadersTest extends LocalServerTest {

    @Test
    public void testAddHeadersToRequest() throws IOException {
        HttpGet httpGet = new HttpGet(getLocalServerHostnameAndPort() + "/echo");
        proxy.addHeader("testheader1", "testvalue1");
        proxy.addHeader("testheader2", "testvalue2");
        String body = IOUtils.toStringAndClose(client.execute(httpGet).getEntity().getContent());

        assertTrue(body.contains("testheader1: testvalue1"));
        assertTrue(body.contains("testheader2: testvalue2"));
    }

    @Test
    public void testCanChangePreviouslyAddedHeaders() throws IOException {
        HttpGet httpGet = new HttpGet(getLocalServerHostnameAndPort() + "/echo");
        proxy.addHeader("testheader1", "testvalue1");
        proxy.addHeader("testheader2", "testvalue2");
        IOUtils.toStringAndClose(client.execute(httpGet).getEntity().getContent());

        proxy.addHeader("testheader1", "newvalue1");
        proxy.addHeader("testheader2", "newvalue2");

        String body = IOUtils.toStringAndClose(client.execute(httpGet).getEntity().getContent());

        assertTrue(body.contains("testheader1: newvalue1"));
        assertTrue(body.contains("testheader2: newvalue2"));

    }
}
