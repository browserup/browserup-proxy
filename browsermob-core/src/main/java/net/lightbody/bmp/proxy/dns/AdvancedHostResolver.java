package net.lightbody.bmp.proxy.dns;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This interface defines the "core" DNS-manipulation functionality that BrowserMob Proxy supports, in addition to the basic name resolution
 * capability defined in {@link net.lightbody.bmp.proxy.dns.HostResolver}.
 */
public interface AdvancedHostResolver extends HostResolver {
    /**
     * <b>Adds</b> the host remappings in the specified Map of {@code <original hostname, remapped hostname>} to the existing list of remappings (if any).
     * <p/>
     * <b>Note:</b> The original hostnames must exactly match the requested hostname. It is not a domain or regular expression match.
     *
     * @param hostRemappings Map of {@code <original hostname, remapped hostname>}
     */
    void remapHosts(Map<String, String> hostRemappings);

    /**
     * Remaps an individual host.
     *
     * @param originalHost Original host to remap. Must exactly match the requested hostname (not a domain or regular expression match).
     * @param remappedHost hostname that will replace originalHost
     */
    void remapHost(String originalHost, String remappedHost);

    /**
     * Removes the specified host remapping. If the remapping does not exist, this method has no effect.
     *
     * @param originalHost currently-remapped hostname
     */
    void removeHostRemapping(String originalHost);

    /**
     * Removes all hostname remappings.
     */
    void clearHostRemappings();

    /**
     * Returns all host remappings in effect.
     *
     * @return Map of {@code <original hostname, remapped hostname>}
     */
    Map<String, String> getHostRemappings();

    /**
     * Clears the existing DNS cache.
     */
    void clearDNSCache();

    /**
     * Sets the timeout when making DNS lookups.
     *
     * @param timeout maximum lookup time
     * @param timeUnit units of the timeout value
     */
    void setDNSCacheTimeout(int timeout, TimeUnit timeUnit);
}
