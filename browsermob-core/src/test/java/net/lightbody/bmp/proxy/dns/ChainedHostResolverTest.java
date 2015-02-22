package net.lightbody.bmp.proxy.dns;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChainedHostResolverTest {
    private static final InetAddress ones;
    private static final InetAddress twos;
    private static final List<InetAddress> firstList;
    private static final List<InetAddress> secondList;
    static {
        try {
            ones = InetAddress.getByName("1.1.1.1");
            twos = InetAddress.getByName("2.2.2.2");
            firstList = ImmutableList.of(ones);
            secondList = ImmutableList.of(twos);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEmptyResolver() {
        ChainedHostResolver resolver = new ChainedHostResolver(null);

        Collection<InetAddress> results = resolver.resolve("www.google.com");

        assertNotNull("Resolver should not return null results", results);
        assertTrue("Empty resolver chain should return empty results", results.isEmpty());

        Map<String, String> remappings = resolver.getHostRemappings();
        assertTrue("Empty resolver chain should return empty results", remappings.isEmpty());

        // make sure no exception is thrown when attempting write operations on an empty resolver
        resolver.setNegativeDNSCacheTimeout(1000, TimeUnit.DAYS);
        resolver.setPositiveDNSCacheTimeout(1000, TimeUnit.DAYS);
        resolver.clearDNSCache();
        resolver.remapHost("", "");
        resolver.remapHosts(ImmutableMap.of("", ""));
        resolver.removeHostRemapping("");
        resolver.clearHostRemappings();
    }

    @Test
    public void testResolveReturnsFirstResults() {
        AdvancedHostResolver firstResolver = mock(AdvancedHostResolver.class);
        AdvancedHostResolver secondResolver = mock(AdvancedHostResolver.class);
        ChainedHostResolver chainResolver = new ChainedHostResolver(ImmutableList.of(firstResolver, secondResolver));

        when(firstResolver.resolve("1.1.1.1")).thenReturn(firstList);
        when(secondResolver.resolve("1.1.1.1")).thenReturn(Collections.<InetAddress>emptyList());

        Collection<InetAddress> results = chainResolver.resolve("1.1.1.1");
        assertNotNull("Resolver should not return null results", results);
        assertFalse("Expected resolver to return a result", results.isEmpty());
        assertEquals("Resolver returned unexpected result", ones, Iterables.get(results, 0));

        verify(secondResolver, never()).resolve("1.1.1.1");

        reset(firstResolver);
        reset(secondResolver);

        when(firstResolver.resolve("2.2.2.2")).thenReturn(Collections.<InetAddress>emptyList());
        when(secondResolver.resolve("2.2.2.2")).thenReturn(secondList);

        results = chainResolver.resolve("2.2.2.2");
        assertNotNull("Resolver should not return null results", results);
        assertFalse("Expected resolver to return a result", results.isEmpty());
        assertEquals("Resolver returned unexpected result", twos, Iterables.get(results, 0));

        verify(firstResolver).resolve("2.2.2.2");
        verify(secondResolver).resolve("2.2.2.2");
    }

    @Test
    public void testGetterUsesFirstResolver() {
        AdvancedHostResolver firstResolver = mock(AdvancedHostResolver.class);
        AdvancedHostResolver secondResolver = mock(AdvancedHostResolver.class);
        ChainedHostResolver chainResolver = new ChainedHostResolver(ImmutableList.of(firstResolver, secondResolver));

        when(firstResolver.getOriginalHostnames("one")).thenReturn(ImmutableList.of("originalOne"));

        Collection<String> results = chainResolver.getOriginalHostnames("one");
        assertNotNull("Resolver should not return null results", results);
        assertFalse("Expected resolver to return a result", results.isEmpty());
        assertEquals("Resolver returned unexpected result", "originalOne", Iterables.get(results, 0));

        verify(secondResolver, never()).getOriginalHostnames(any(String.class));
    }

    @Test
    public void testResolveWaitsForWriteOperation() throws InterruptedException {
        AdvancedHostResolver firstResolver = mock(AdvancedHostResolver.class);
        AdvancedHostResolver secondResolver = mock(AdvancedHostResolver.class);
        final ChainedHostResolver chainResolver = new ChainedHostResolver(ImmutableList.of(firstResolver, secondResolver));

        final AtomicBoolean secondResolverClearingCache = new AtomicBoolean(false);
        // track the time when the second resolver finishes clearing the cache, so we can verify the simultaneous resolve in the
        // first resolver starts AFTER the cache clear completes
        final AtomicLong secondResolverCacheClearFinishedTime = new AtomicLong(0);

        // set up the second resolver to sleep for a few seconds when a clearDNSCache() call is made
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                secondResolverClearingCache.set(true);

                Thread.sleep(4000);

                secondResolverCacheClearFinishedTime.set(System.nanoTime());

                return null;
            }
        }).when(secondResolver).clearDNSCache();

        // track the time the first resolver starts resolving the address, to make sure it is AFTER the DNS cache clear time
        final AtomicLong firstResolverStartedResolvingTime = new AtomicLong(0);

        // set up the first resolver to capture the time it starts resolving the address
        when(firstResolver.resolve("1.1.1.1")).then(new Answer<Collection<InetAddress>>() {
            @Override
            public Collection<InetAddress> answer(InvocationOnMock invocationOnMock) throws Throwable {
                firstResolverStartedResolvingTime.set(System.nanoTime());
                return firstList;
            }
        });

        // run the DNS cache clear in a separate thread, so it will be running (and sleeping) when we test the resolve() method
        new Thread(new Runnable() {
            @Override
            public void run() {
                chainResolver.clearDNSCache();
            }
        }).start();

        // wait for the clearDNSCache() call to start executing
        Thread.sleep(1000);

        // make sure the second resolver has started clearing the cache before invoking resolve on the chained resolver, to make sure
        // that this call to resolve will result in a wait
        assertTrue("Expected resolver to already be clearing the DNS cache in a separate thread", secondResolverClearingCache.get());
        assertEquals("Did not expect resolver to already be finished clearing the DNS cache", 0, secondResolverCacheClearFinishedTime.get());

        Collection<InetAddress> results = chainResolver.resolve("1.1.1.1");

        assertNotNull("Resolver should not return null results", results);
        assertFalse("Expected resolver to return a result", results.isEmpty());
        assertEquals("Resolver returned unexpected result", ones, Iterables.get(results, 0));

        assertTrue("Expected resolver to be finished clearing DNS cache", secondResolverCacheClearFinishedTime.get() > 0);

        assertTrue("Expected resolver to finish clearing the DNS cache before starting to resolve an address", firstResolverStartedResolvingTime.get() > secondResolverCacheClearFinishedTime.get());
    }
}
