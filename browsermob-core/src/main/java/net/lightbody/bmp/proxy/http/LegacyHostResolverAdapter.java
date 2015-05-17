package net.lightbody.bmp.proxy.http;

import net.lightbody.bmp.proxy.dns.AdvancedHostResolver;
import org.apache.http.conn.DnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * An adapter that allows the legacy {@link net.lightbody.bmp.proxy.http.BrowserMobHttpClient} to use the new
 * {@link net.lightbody.bmp.proxy.dns.AdvancedHostResolver} implementations. In addition to implementing the
 * {@link org.apache.http.conn.DnsResolver} interface that BrowserMobHttpClient needs, this adapter also populates timing and address
 * info in the RequestInfo class.
 */
public class LegacyHostResolverAdapter implements DnsResolver {
    private volatile AdvancedHostResolver resolver;

    public LegacyHostResolverAdapter(AdvancedHostResolver resolver) {
        this.resolver = resolver;
    }

    public void setResolver(AdvancedHostResolver resolver) {
        this.resolver = resolver;
    }

    public AdvancedHostResolver getResolver() {
        return resolver;
    }

    @Override
    public InetAddress[] resolve(String s) throws UnknownHostException {
        long start = System.nanoTime();

        InetAddress[] addresses = resolver.resolve(s).toArray(new InetAddress[0]);
        if (addresses.length == 0) {
            throw new UnknownHostException(s);
        }

        long end = System.nanoTime();

        // Associate the the host name with the connection. We do this because when using persistent
        // connections there won't be a lookup on the 2nd, 3rd, etc requests, and as such we wouldn't be able to
        // know what IP address we were requesting.
        RequestInfo.get().dns(start, end, addresses[0].getHostAddress());

        return addresses;
    }
}
