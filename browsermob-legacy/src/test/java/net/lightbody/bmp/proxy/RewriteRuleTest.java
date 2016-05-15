package net.lightbody.bmp.proxy;

import net.lightbody.bmp.proxy.test.util.LocalServerTest;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RewriteRuleTest extends LocalServerTest {

    @Test
    public void testThatRewriteRulesCanBeCleared() throws IllegalStateException, ClientProtocolException, IOException {
        proxy.rewriteUrl("(.*)a\\.txt", "$1b.txt");
        // make surethe rewrite rules are working
        String body = IOUtils.toStringAndClose(client.execute(new HttpGet(getLocalServerHostnameAndPort() + "/a.txt")).getEntity().getContent());
        assertThat(body, equalTo("this is b.txt"));
        // check that clearing them works
        proxy.clearRewriteRules();
        body = IOUtils.toStringAndClose(client.execute(new HttpGet(getLocalServerHostnameAndPort() + "/a.txt")).getEntity().getContent());
        assertThat(body, equalTo("this is a.txt"));
    }

}
