package net.lightbody.bmp.proxy.dns;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

@RunWith(Parameterized.class)
public class AdvancedHostResolverCacheTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // skip DNS cache operations for NativeResolver
                {DnsJavaResolver.class}, {NativeCacheManipulatingResolver.class}
        });
    }

    public AdvancedHostResolver resolver;

    public AdvancedHostResolverCacheTest(Class<AdvancedHostResolver> resolverClass) throws IllegalAccessException, InstantiationException {
        this.resolver = resolverClass.newInstance();
    }

    @Before
    public void skipForTravisCi() {
        // skip these tests on the CI server since the DNS lookup is extremely fast, even when cached
        assumeFalse("true".equals(System.getenv("TRAVIS")));
    }

    @Test
    public void testCanClearDNSCache() {
        // populate the cache
        resolver.resolve("www.msn.com");

        resolver.clearDNSCache();

        long start = System.nanoTime();
        resolver.resolve("www.msn.com");
        long finish = System.nanoTime();

        assertNotEquals("Expected non-zero DNS lookup time for www.msn.com after clearing DNS cache", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testCachedPositiveLookup() {
        long start = System.nanoTime();
        // must use an address that we haven't already resolved in another test
        resolver.resolve("news.bing.com");
        long finish = System.nanoTime();

        long uncachedLookupMs = TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS);

        assertNotEquals("Expected non-zero DNS lookup time for news.bing.com on first lookup", 0, uncachedLookupMs);

        start = System.nanoTime();
        resolver.resolve("news.bing.com");
        finish = System.nanoTime();

        long cachedLookupMs = TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS);

        assertTrue("Expected extremely fast DNS lookup time for news.bing.com on second (cached) lookup. Uncached: " + uncachedLookupMs + "ms; cached: " + cachedLookupMs + "ms.", cachedLookupMs <= uncachedLookupMs / 2);
    }

    @Test
    public void testCachedNegativeLookup() {
        long start = System.nanoTime();
        resolver.resolve("fake.notarealaddress");
        long finish = System.nanoTime();

        long uncachedLookupMs = TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS);

        assertNotEquals("Expected non-zero DNS lookup time for fake.notarealaddress on first lookup", 0, uncachedLookupMs);

        start = System.nanoTime();
        resolver.resolve("fake.notarealaddress");
        finish = System.nanoTime();

        long cachedLookupMs = TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS);

        assertTrue("Expected extremely fast DNS lookup time for fake.notarealaddress on second (cached) lookup. Uncached: " + uncachedLookupMs + "ms; cached: " + cachedLookupMs + "ms.", cachedLookupMs <= uncachedLookupMs / 2);
    }

    @Test
    public void testSetPositiveCacheTtl() throws InterruptedException {
        resolver.clearDNSCache();
        resolver.setPositiveDNSCacheTimeout(2, TimeUnit.SECONDS);

        // populate the cache
        Collection<InetAddress> addresses = resolver.resolve("www.msn.com");

        // make sure there are addresses, since this is a *positive* TTL test
        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find addresses for www.msn.com", 0, addresses.size());

        // wait for the cache to clear
        Thread.sleep(2500);

        long start = System.nanoTime();
        addresses = resolver.resolve("www.msn.com");
        long finish = System.nanoTime();

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find addresses for www.msn.com", 0, addresses.size());

        assertNotEquals("Expected non-zero DNS lookup time for www.msn.com after setting positive cache TTL", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testSetNegativeCacheTtl() throws InterruptedException {
        Random random = new Random();
        String fakeAddress = random.nextInt() + ".madeup.thisisafakeaddress";

        resolver.clearDNSCache();
        resolver.setNegativeDNSCacheTimeout(2, TimeUnit.SECONDS);

        // populate the cache
        Collection<InetAddress> addresses = resolver.resolve(fakeAddress);

        // make sure there are no addresses, since this is a *negative* TTL test
        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertEquals("Expected to find no addresses for " + fakeAddress, 0, addresses.size());

        // wait for the cache to clear
        Thread.sleep(2500);

        long start = System.nanoTime();
        addresses = resolver.resolve(fakeAddress);
        long finish = System.nanoTime();

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertEquals("Expected to find no addresses for " + fakeAddress, 0, addresses.size());

        assertNotEquals("Expected non-zero DNS lookup time for " + fakeAddress + " after setting negative cache TTL", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testSetEternalNegativeCacheTtl() {
        Random random = new Random();
        String fakeAddress = random.nextInt() + ".madeup.thisisafakeaddress";

        resolver.clearDNSCache();
        resolver.setNegativeDNSCacheTimeout(-1, TimeUnit.SECONDS);

        // populate the cache
        Collection<InetAddress> addresses = resolver.resolve(fakeAddress);

        // make sure there are no addresses, since this is a *negative* TTL test
        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertEquals("Expected to find no addresses for " + fakeAddress, 0, addresses.size());

        long start = System.nanoTime();
        addresses = resolver.resolve(fakeAddress);
        long finish = System.nanoTime();

        long cachedLookupMs = TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS);

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertEquals("Expected to find no addresses for " + fakeAddress, 0, addresses.size());

        assertTrue("Expected extremely fast DNS lookup time for " + fakeAddress + " after setting eternal negative cache TTL. Cached lookup time: " + cachedLookupMs + "ms.", cachedLookupMs <= 10);
    }

    @Test
    public void testSetEternalPositiveCacheTtl() {
        resolver.clearDNSCache();
        resolver.setPositiveDNSCacheTimeout(-1, TimeUnit.SECONDS);

        System.out.println("Resolver: " + resolver);

        // populate the cache
        long one = System.nanoTime();
        Collection<InetAddress> addresses = resolver.resolve("www.msn.com");
        long two = System.nanoTime();
        System.out.println("Time to resolve without cache: " + TimeUnit.MILLISECONDS.convert(two - one, TimeUnit.NANOSECONDS));

        // make sure there are addresses, since this is a *positive* TTL test
        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find addresses for www.msn.com", 0, addresses.size());

        long start = System.nanoTime();
        addresses = resolver.resolve("www.msn.com");
        long finish = System.nanoTime();

        long cachedLookupMs = TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS);

        System.out.println("Time to resolve with cache: " + TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find addresses for www.msn.com", 0, addresses.size());

        assertTrue("Expected extremely fast DNS lookup time for www.msn.com after setting eternal negative cache TTL. Cached lookup time: " + cachedLookupMs + "ms.", cachedLookupMs <= 10);
    }
}
