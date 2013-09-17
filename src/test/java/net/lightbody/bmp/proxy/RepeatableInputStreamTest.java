package net.lightbody.bmp.proxy;

import junit.framework.Assert;
import net.lightbody.bmp.proxy.http.BrowserMobHttpRequest;
import net.lightbody.bmp.proxy.http.RequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;

public class RepeatableInputStreamTest extends DummyServerTest{

    @Test
    public void test() {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();

        proxy.addRequestInterceptor(testRequestInterceptor);

        HttpResponse response = null;
        try {
            response = client.execute(new HttpGet("http://127.0.0.1:8080/echo"));
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {

            EntityUtils.consumeQuietly(response.getEntity());
        }

        int responseCode = response.getStatusLine().getStatusCode();
        Assert.assertTrue(responseCode == 200);
        Assert.assertNotNull(testRequestInterceptor.getBrowserMobHttpRequest());
        Assert.assertNotNull(testRequestInterceptor.getBrowserMobHttpRequest().getInputStreamEntity());
        Assert.assertTrue(testRequestInterceptor.getBrowserMobHttpRequest().getInputStreamEntity().isRepeatable());
    }

    class TestRequestInterceptor implements RequestInterceptor{
        BrowserMobHttpRequest _browserMobHttpRequest;

        public BrowserMobHttpRequest getBrowserMobHttpRequest(){
            return _browserMobHttpRequest;
        }

        @Override
        public void process(BrowserMobHttpRequest request) {
            _browserMobHttpRequest = request;
        }
    }
}
