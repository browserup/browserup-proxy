package com.browserup.bup.proxy.dns;

import com.google.common.collect.ImmutableList;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Collections.*;

/**
 * An {@link com.browserup.bup.proxy.dns.AdvancedHostResolver} that applies the AdvancedHostResolver methods to multiple implementations. Methods
 * are applied to the resolvers in the order specified when the ChainedHostResolver is constructed. AdvancedHostResolver methods that modify the
 * resolver are guaranteed to complete atomically over all resolvers. For example, if one thread makes a call to
 * {@link #resolve(String)} while another thread is remapping hosts using
 * {@link #remapHost(String, String)}, the call to {@link #resolve(String)} is guaranteed to
 * apply the newly-remapped hosts to <i>all</i> resolvers managed by this ChainedHostResolver, or to <i>no</i> resolvers, but the call to
 * {@link #resolve(String)} will never result in the host name remappings applied only to "some" of the chained resolvers.
 * For getter methods (all read-only methods except {@link #resolve(String)}), the ChainedHostResolver returns results from the first chained resolver.
 * The atomic write methods specified by AdvancedHostResolver are:
 * <ul>
 *     <li>{@link com.browserup.bup.proxy.dns.AdvancedHostResolver#remapHost(String, String)}</li>
 *     <li>{@link com.browserup.bup.proxy.dns.AdvancedHostResolver#remapHosts(java.util.Map)}</li>
 *     <li>{@link com.browserup.bup.proxy.dns.AdvancedHostResolver#removeHostRemapping(String)}</li>
 *     <li>{@link com.browserup.bup.proxy.dns.AdvancedHostResolver#clearHostRemappings()}</li>
 *     <li>{@link com.browserup.bup.proxy.dns.AdvancedHostResolver#setNegativeDNSCacheTimeout(int, java.util.concurrent.TimeUnit)}</li>
 *     <li>{@link com.browserup.bup.proxy.dns.AdvancedHostResolver#setPositiveDNSCacheTimeout(int, java.util.concurrent.TimeUnit)}</li>
 *     <li>{@link com.browserup.bup.proxy.dns.AdvancedHostResolver#clearDNSCache()}</li>
 * </ul>
 */
public class ChainedHostResolver implements AdvancedHostResolver {
    private final List<? extends AdvancedHostResolver> resolvers;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * Creates a ChainedHostResolver that applies {@link com.browserup.bup.proxy.dns.AdvancedHostResolver} methods to the specified resolvers
     * in the order specified by the collection's iterator.
     *
     * @param resolvers resolvers to invoke, in the order specified by the collection's iterator
     */
    public ChainedHostResolver(Collection<? extends AdvancedHostResolver> resolvers) {
        if (resolvers == null) {
            this.resolvers = emptyList();
        } else {
            this.resolvers = ImmutableList.copyOf(resolvers);
        }
    }

    /**
     * Returns the resolvers used by this ChainedHostResolver. The iterator of the collection is guaranteed to return the resolvers in the order
     * in which they are queried.
     *
     * @return resolvers used by this ChainedHostResolver
     */
    public Collection<? extends AdvancedHostResolver> getResolvers() {
        return ImmutableList.copyOf(resolvers);
    }

    @Override
    public void remapHosts(Map<String, String> hostRemappings) {
        writeLock.lock();
        try {
            resolvers.forEach(resolver -> resolver.remapHosts(hostRemappings));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remapHost(String originalHost, String remappedHost) {
        writeLock.lock();
        try {
            resolvers.forEach(resolver -> resolver.remapHost(originalHost, remappedHost));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeHostRemapping(String originalHost) {
        writeLock.lock();
        try {
            resolvers.forEach(resolver -> resolver.removeHostRemapping(originalHost));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clearHostRemappings() {
        writeLock.lock();
        try {
            resolvers.forEach(AdvancedHostResolver::clearHostRemappings);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Map<String, String> getHostRemappings() {
        readLock.lock();
        try {
            if (resolvers.isEmpty()) {
                return emptyMap();
            } else {
                return resolvers.get(0).getHostRemappings();
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<String> getOriginalHostnames(String remappedHost) {
        readLock.lock();
        try {
            if (resolvers.isEmpty()) {
                return emptyList();
            } else {
                return resolvers.get(0).getOriginalHostnames(remappedHost);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void clearDNSCache() {
        writeLock.lock();
        try {
            resolvers.forEach(AdvancedHostResolver::clearDNSCache);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void setPositiveDNSCacheTimeout(int timeout, TimeUnit timeUnit) {
        writeLock.lock();
        try {
            resolvers.forEach(resolver -> resolver.setPositiveDNSCacheTimeout(timeout, timeUnit));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void setNegativeDNSCacheTimeout(int timeout, TimeUnit timeUnit) {
        writeLock.lock();
        try {
            resolvers.forEach(resolver -> resolver.setNegativeDNSCacheTimeout(timeout, timeUnit));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Collection<InetAddress> resolve(String host) {
        readLock.lock();
        try {
            // attempt to resolve the host using all resolvers. returns the results from the first successful resolution.
            return resolvers.stream()
                    .map(resolver -> resolver.resolve(host))
                    .filter(results -> !results.isEmpty())
                    .findFirst()
                    .orElse(emptyList());

            // no resolvers returned results
        } finally {
            readLock.unlock();
        }
    }
}
