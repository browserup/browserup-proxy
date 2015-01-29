package net.lightbody.bmp.proxy;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeThat;

/**
 * Tests to exercise blacklist and whitelist functionality
 *
 * @author Andy Clark (andy.clark@realvnc.com)
 *
 */
public class BlackAndWhiteListTest extends DummyServerTest {

	/*
	 * Some tests were hanging when trying to GET un-whitelisted URLs.
	 * Implementing a timeouts prevents these from blocking a test indefinitely.
	 *
	 * Applied to each test, rather with a global Timeout rule, as global
	 * timeout rule was preventing parent's @After rule from running, and dummy
	 * server from being shut down.
	 */

    /**
     * Checks that the modified status code is returned for blacklisted URLs,
     * but that the original one is returned for URLs that don't match the
     * blacklisting pattern.
     */
    @Test
    public void testStatusCodeIsReturnedOnBlacklist()
            throws ClientProtocolException, IOException {
        proxy.blacklistRequests(".*a\\.txt.*", 500);
        assertThat("Unexpected status code for unblacklisted URL",
                httpStatusWhenGetting(getHostnameAndPort() + "/b.txt"), is(200));
        assertThat("Unexpected status code for blacklisted URL",
                httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(500));
    }

    /**
     * Checks that the alternative status is returned for all but whitelisted
     * URLs.
     */
    @Test
    public void testStatusCodeIsReturnedOnWhitelist()
            throws ClientProtocolException, IOException {
        proxy.whitelistRequests(new String[] { ".*a\\.txt.*", ".*\\.png" }, 500);
        assertThat("Unexpected status code for whitelisted URL, first entry",
                httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"),
                is(not(500)));
        assertThat("Unexpected status code for whitelisted URL, second entry",
                httpStatusWhenGetting(getHostnameAndPort() + "/c.png"),
                is(not(500)));
        assertThat("Unexpected status code for un-whitelisted URL",
                httpStatusWhenGetting(getHostnameAndPort() + "/b.txt"), is(500));
    }

    /**
     * Check that an entry in the blacklist will override the status code
     * returned, even if the URL matches a pattern of an entry in the whitelist.
     */
    @Test
    public void testBlacklistOverridesWhitelist()
            throws ClientProtocolException, IOException {
        int NON_WHITE_CODE = 500;
        int BLACK_CODE_1 = 400;
        int BLACK_CODE_2 = 404;
        int NORMAL_CODE = 200;
        proxy.whitelistRequests(new String[] { ".*\\.txt" }, NON_WHITE_CODE);
        proxy.blacklistRequests(".*b\\.txt", BLACK_CODE_1);
        proxy.blacklistRequests(".*\\.gz", BLACK_CODE_2);

        // whitelisted URL gets normal status code
        assertThat("Unexpected status code from whitelisted URL",
                httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"),
                is(NORMAL_CODE));

        // should get normal status as whitelisted, but blacklist kicks in
        assertThat("Unexpected status code for blacklisted & whitelisted URL",
                httpStatusWhenGetting(getHostnameAndPort() + "/b.txt"),
                is(BLACK_CODE_1));

        // not on the whitelist, so should get NON_WHITE_CODE, but blacklist
        // should kick in and prevent that.
        assertThat(
                "Unexpeced status code for non-whitelisted, blacklisted URL",
                httpStatusWhenGetting(getHostnameAndPort() + "/a.txt.gz"),
                is(BLACK_CODE_2));

        // not whitelisted, not blacklisted, so gets non-whitelist code
        assertThat("Unexpected status code for un-whitelisted URL",
                httpStatusWhenGetting(getHostnameAndPort() + "/c.png"),
                is(NON_WHITE_CODE));
    }

    /**
     * Checks that a proxy whitelist can be cleared successfully.
     */
    @Test
    public void testWhitelistCanBeCleared() throws ClientProtocolException, IOException {
        proxy.whitelistRequests(new String[] { ".*\\.txt" }, 500);
        // assume that proxy is working before
        assumeThat(httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(200));
        assumeThat(httpStatusWhenGetting(getHostnameAndPort() + "/c.png"), is(500));
        // clear the whitelist
        proxy.clearWhitelist();
        // check that no whitelist is in effect
        assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(200));
        assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/c.png"), is(200));
    }
    
    @Test
    public void testWhitelistCanBeReplaced() throws ClientProtocolException, IOException {
    	proxy.whitelistRequests(new String[] { ".*\\.txt" }, 404);

        // test that the whitelist is working
        assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(200));
        assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/c.png"), is(404));
        
        proxy.whitelistRequests(new String[] { ".*\\.png" }, 404);
        
        // check that the new whitelist is working and the old is gone
        assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(404));
        assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/c.png"), is(200));
    }
    
    @Test
    public void testEmptyWhitelist() throws ClientProtocolException, IOException {
    	assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(200));
    	
    	proxy.enableEmptyWhitelist(404);
    	
    	assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(404));
    }
    
    @Test
    public void testWhitelistIsDisabledByDefault() {
    	assertFalse("whitelist should be diabled unless explicitly set", proxy.isWhitelistEnabled());
    }

    /**
     * Checks that a proxy blacklist can be cleared successfully.
     */
    @Test
    public void testBlacklistCanBeCleared() throws ClientProtocolException, IOException {
        proxy.blacklistRequests(".*\\.txt", 404);
        // assume that proxy is working before
        assumeThat(httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(404));
        assumeThat(httpStatusWhenGetting(getHostnameAndPort() + "/c.png"), is(200));
        // clear the blacklist
        proxy.clearBlacklist();
        // check that no blacklist is in effect
        assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/a.txt"), is(200));
        assertThat(httpStatusWhenGetting(getHostnameAndPort() + "/c.png"), is(200));
    }

    @Test
    public void testFacebook() throws IOException {
        proxy.blacklistRequests("https?://.*\\.facebook\\.com/.*", 678);

        assertThat("Unexpected status code from blacklisted URL",
                httpStatusWhenGetting("http://www.facebook.com/something-not-really-there"),
                is(678));

    }


    /**
     * Makes a HTTP Get request to the supplied URI, and returns the HTTP status
     * code.
     * <p>
     * Consumes any HTTP response body to prevent subsequent calls from hanging.
     */
    protected int httpStatusWhenGetting(String uri)
            throws ClientProtocolException, IOException {
        HttpResponse response = null;
        try {
            response = client.execute(new HttpGet(uri));
            return response.getStatusLine().getStatusCode();
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

}