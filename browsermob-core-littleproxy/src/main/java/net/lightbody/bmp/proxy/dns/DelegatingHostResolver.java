package net.lightbody.bmp.proxy.dns;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * A HostResolver that delegates to the specified {@link net.lightbody.bmp.proxy.dns.HostResolver} instances. This class will use the
 * first resolved InetAddress, invoking the specified HostResolvers in order. This class can be used instead
 * of {@link net.lightbody.bmp.proxy.dns.ChainedHostResolver} if clients need fine-grained control over DNS resolution.
*/
public class DelegatingHostResolver implements org.littleshoot.proxy.HostResolver {
    private volatile Collection<? extends HostResolver> resolvers;

    /**
     * Creates a new resolver that will delegate to the specified resolvers in the order determined by the Collection's iterator. This class
     * does not make a defensive copy of the Collection, so any changes to the Collection will be reflected in subsequent calls to {@link #resolve(String, int)}.
     *
     * @param resolvers HostResolvers to delegate to
     */
    public DelegatingHostResolver(Collection<? extends HostResolver> resolvers) {
        this.resolvers = resolvers;
    }

    /**
     * Creates a new delegating resolver that does not actually delegate to any resolver ({@link #resolve(String, int)} will always throw UnknownHostException).
     */
    public DelegatingHostResolver() {
        this(ImmutableList.<HostResolver>of());
    }

    public Collection<? extends HostResolver> getResolvers() {
        return resolvers;
    }

    public void setResolvers(Collection<? extends HostResolver> resolvers) {
        this.resolvers = resolvers;
    }

    @Override
    public InetSocketAddress resolve(String host, int port) throws UnknownHostException {
        for (HostResolver resolver : resolvers) {
            Collection<InetAddress> resolvedAddresses = resolver.resolve(host);
            if (!resolvedAddresses.isEmpty()) {
                InetAddress resolvedAddress = Iterables.get(resolvedAddresses, 0);
                return new InetSocketAddress(resolvedAddress, port);
            }
        }

        // no address found by any resolver
        throw new UnknownHostException(host);
    }
}
