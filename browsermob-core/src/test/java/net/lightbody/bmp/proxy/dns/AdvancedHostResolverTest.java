package net.lightbody.bmp.proxy.dns;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class AdvancedHostResolverTest {
    private static final Logger log = LoggerFactory.getLogger(AdvancedHostResolverTest.class);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {DnsJavaResolver.class}, {NativeResolver.class}, {NativeCacheManipulatingResolver.class}
        });
    }

    public AdvancedHostResolver resolver;

    public AdvancedHostResolverTest(Class<AdvancedHostResolver> resolverClass) throws IllegalAccessException, InstantiationException {
        this.resolver = resolverClass.newInstance();
    }

    private boolean ipv6Enabled = false;

    @Before
    public void testForIPv6() throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName("::1");
        if (addresses != null) {
            for (InetAddress addr : addresses) {
                if (addr.getClass() == Inet6Address.class) {
                    ipv6Enabled = true;

                    return;
                }
            }
        }
    }

    @Test
    public void testResolveAddress() {
        Collection<InetAddress> yahooAddresses = resolver.resolve("www.yahoo.com");

        assertNotNull("Collection of resolved addresses should never be null", yahooAddresses);

        assertNotEquals("Expected to find at least one address for www.yahoo.com", 0, yahooAddresses.size());
    }

    @Test
    public void testCannotResolveAddress() {
        Collection<InetAddress> noAddresses = resolver.resolve("www.notarealaddress.grenyarnia");

        assertNotNull("Collection of resolved addresses should never be null", noAddresses);

        assertEquals("Expected to find no address for www.notarealaddress.grenyarnia", 0, noAddresses.size());
    }

    @Test
    public void testResolveIPv4AndIPv6Addresses() {
        assumeTrue("Skipping test because IPv6 is not enabled", ipv6Enabled);

        boolean foundIPv4 = false;
        boolean foundIPv6 = false;
        Collection<InetAddress> addresses = resolver.resolve("www.google.com");
        for (InetAddress address : addresses) {
            if (address.getClass() == Inet4Address.class) {
                foundIPv4 = true;
            } else if (address.getClass() == Inet6Address.class) {
                foundIPv6 = true;
            }
        }

        assertTrue("Expected to find at least one IPv4 address for www.google.com", foundIPv4);

        // disabling this assert to prevent test failures on systems without ipv6 access, or when the DNS server does not return IPv6 addresses
        //assertTrue("Expected to find at least one IPv6 address for www.google.com", foundIPv6);
        if (!foundIPv6) {
            log.warn("Could not resolve IPv6 address for www.google.com using resolver {}", resolver.getClass().getSimpleName());
        }

    }

    @Test
    public void testCanClearDNSCache() {
        // skip DNS cache operations for NativeResolver
        if (resolver.getClass() == NativeResolver.class) {
            return;
        }

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
        // skip this test for NativeCacheManipulatingResolver, since its lookup behavior is the same as NativeResolver, and they will share the same InetAddress cache
        if (resolver.getClass() == NativeCacheManipulatingResolver.class) {
            return;
        }

        long start = System.nanoTime();
        // must use an address that we haven't already resolved in another test
        resolver.resolve("news.bing.com");
        long finish = System.nanoTime();

        assertNotEquals("Expected non-zero DNS lookup time for news.bing.com on first lookup", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));

        start = System.nanoTime();
        resolver.resolve("news.bing.com");
        finish = System.nanoTime();

        assertEquals("Expected instant DNS lookup time for news.bing.com on second (cached) lookup", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testCachedNegativeLookup() {
        // skip this test for NativeCacheManipulatingResolver, since its lookup behavior is the same as NativeResolver, and they will share the same InetAddress cache
        if (resolver.getClass() == NativeCacheManipulatingResolver.class) {
            return;
        }

        long start = System.nanoTime();
        resolver.resolve("fake.notarealaddress");
        long finish = System.nanoTime();

        assertNotEquals("Expected non-zero DNS lookup time for fake.notarealaddress on first lookup", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));

        start = System.nanoTime();
        resolver.resolve("fake.notarealaddress");
        finish = System.nanoTime();

        assertEquals("Expected instant DNS lookup time for fake.notarealaddress on second (cached) lookup", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testSetPositiveCacheTtl() throws InterruptedException {
        // skip DNS cache operations for NativeResolver
        if (resolver.getClass() == NativeResolver.class) {
            return;
        }

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
        // skip DNS cache operations for NativeResolver
        if (resolver.getClass() == NativeResolver.class) {
            return;
        }

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
        // skip DNS cache operations for NativeResolver
        if (resolver.getClass() == NativeResolver.class) {
            return;
        }

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

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertEquals("Expected to find no addresses for " + fakeAddress, 0, addresses.size());

        assertEquals("Expected instant DNS lookup time for " + fakeAddress + " after setting eternal negative cache TTL", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testSetEternalPositiveCacheTtl() {
        // skip DNS cache operations for NativeResolver
        if (resolver.getClass() == NativeResolver.class) {
            return;
        }

        resolver.clearDNSCache();
        resolver.setPositiveDNSCacheTimeout(-1, TimeUnit.SECONDS);

        // populate the cache
        Collection<InetAddress> addresses = resolver.resolve("www.msn.com");

        // make sure there are addresses, since this is a *positive* TTL test
        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find addresses for www.msn.com", 0, addresses.size());

        long start = System.nanoTime();
        addresses = resolver.resolve("www.msn.com");
        long finish = System.nanoTime();

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find addresses for www.msn.com", 0, addresses.size());

        assertEquals("Expected instant DNS lookup time for www.msn.com after setting eternal positive cache TTL", 0, TimeUnit.MILLISECONDS.convert(finish - start, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testResolveLocalhost() {
        // DnsJavaResolver cannot resolve localhost, since it does not look up entries in the hosts file
        if (resolver.getClass() == DnsJavaResolver.class) {
            return;
        }

        Collection<InetAddress> addresses = resolver.resolve("localhost");

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find at least one address for localhost", 0, addresses.size());
    }

    @Test
    public void testResolveIPv4Address() {
        Collection<InetAddress> addresses = resolver.resolve("127.0.0.1");

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find at least one address for 127.0.0.1", 0, addresses.size());
    }

    @Test
    public void testResolveIPv6Address() {
        assumeTrue("Skipping test because IPv6 is not enabled", ipv6Enabled);

        Collection<InetAddress> addresses = resolver.resolve("::1");

        assertNotNull("Collection of resolved addresses should never be null", addresses);
        assertNotEquals("Expected to find at least one address for ::1", 0, addresses.size());
    }

    @Test
    public void testResolveRemappedHost() {
        Collection<InetAddress> originalAddresses = resolver.resolve("www.google.com");

        assertNotNull("Collection of resolved addresses should never be null", originalAddresses);
        assertNotEquals("Expected to find at least one address for www.google.com", 0, originalAddresses.size());

        resolver.remapHost("www.google.com", "www.bing.com");

        Collection<InetAddress> remappedAddresses = resolver.resolve("www.google.com");
        assertNotNull("Collection of resolved addresses should never be null", remappedAddresses);
        assertNotEquals("Expected to find at least one address for www.google.com remapped to www.bing.com", 0, remappedAddresses.size());

        InetAddress firstRemappedAddr = remappedAddresses.iterator().next();

        //TODO: verify this is correct -- should remapping return the remapped hostname, or the original hostname but with an IP address corresponding to the remapped hostname?
        assertEquals("Expected hostname for returned address to reflect the remapped address.", "www.bing.com", firstRemappedAddr.getHostName());
    }

    @Test
    public void testRetrieveOriginalHostByRemappedHost() {
        resolver.remapHost("www.google.com", "www.bing.com");

        Collection<String> originalHostnames = resolver.getOriginalHostnames("www.bing.com");
        assertEquals("Expected to find one original hostname after remapping", 1, originalHostnames.size());

        String original = originalHostnames.iterator().next();
        assertEquals("Expected to find original hostname of www.google.com after remapping to www.bing.com", "www.google.com", original);
    }

    @Test
    public void testRemoveHostRemapping() {
        resolver.remapHost("www.google.com", "www.notarealaddress");

        Collection<InetAddress> remappedAddresses = resolver.resolve("www.google.com");
        assertEquals("Expected to find no address for remapped www.google.com", 0, remappedAddresses.size());

        resolver.removeHostRemapping("www.google.com");

        Collection<InetAddress> regularAddress = resolver.resolve("www.google.com");
        assertNotNull("Collection of resolved addresses should never be null", remappedAddresses);
        assertNotEquals("Expected to find at least one address for www.google.com after removing remapping", 0, regularAddress.size());
    }

    @Test
    public void testClearHostRemappings() {
        resolver.remapHost("www.google.com", "www.notarealaddress");

        Collection<InetAddress> remappedAddresses = resolver.resolve("www.google.com");
        assertEquals("Expected to find no address for remapped www.google.com", 0, remappedAddresses.size());

        resolver.clearHostRemappings();

        Collection<InetAddress> regularAddress = resolver.resolve("www.google.com");
        assertNotNull("Collection of resolved addresses should never be null", remappedAddresses);
        assertNotEquals("Expected to find at least one address for www.google.com after removing remapping", 0, regularAddress.size());
    }
}
