package net.lightbody.bmp.proxy;

import java.util.List;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;

import org.apache.http.client.methods.HttpGet;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertEquals(100, entry.getRequest().getHeadersSize());
        Assert.assertEquals(0, entry.getRequest().getBodySize());
        // 226 for Windows, 227 for other platforms? Can it be that simple?
        Assert.assertThat((int)entry.getResponse().getHeadersSize(), anyOf(is(226),is(227)));
        Assert.assertEquals(13, entry.getResponse().getBodySize());
    }

}
