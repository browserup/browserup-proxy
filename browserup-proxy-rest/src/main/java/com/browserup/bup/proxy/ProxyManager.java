/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.exception.AddressInUseException;
import com.browserup.bup.exception.ProxyExistsException;
import com.browserup.bup.exception.ProxyPortsExhaustedException;
import com.browserup.bup.proxy.auth.AuthType;
import com.browserup.bup.util.BrowserUpProxyUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Singleton
public class ProxyManager {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyManager.class);

    private int lastPort;
    private final int minPort;
    private final int maxPort;
    // retain a reference to the Cache to allow the ProxyCleanupTask to .cleanUp(), since asMap() is just a view into the cache.
    // it would seem to make sense to pass the newly-built Cache directly to the ProxyCleanupTask and have it retain a WeakReference to it, and
    // only maintain a reference to the .asMap() result in this class. puzzlingly, however, the Cache can actually get garbage collected
    // before the .asMap() view of it does.
    private final Cache<Integer, BrowserUpProxyServer> proxyCache;
    private final ConcurrentMap<Integer, BrowserUpProxyServer> proxies;

    /**
     * Interval at which expired proxy checks will actively clean up expired proxies. Proxies may still be cleaned up when accessing the
     * proxies map.
     */
    private static final int EXPIRED_PROXY_CLEANUP_INTERVAL_SECONDS = 60;

    // Initialize-on-demand a single thread executor that will create a daemon thread to clean up expired proxies. Since the resulting executor
    // is a singleton, there will at most one thread to service all ProxyManager instances.
    private static class ScheduledExecutorHolder {
        private static final ScheduledExecutorService expiredProxyCleanupExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName("expired-proxy-cleanup-thread");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    // static inner class to prevent leaking ProxyManager instances to the cleanup task
    private static class ProxyCleanupTask implements Runnable {
        // using a WeakReference that will indicate to us when the Cache (and thus its ProxyManager) has been garbage
        // collected, allowing this cleanup task to kill itself
        private final WeakReference<Cache<Integer, BrowserUpProxyServer>> proxyCache;

        public ProxyCleanupTask(Cache<Integer, BrowserUpProxyServer> cache) {
            this.proxyCache = new WeakReference<Cache<Integer, BrowserUpProxyServer>>(cache);
        }

        @Override
        public void run() {
            Cache<Integer, BrowserUpProxyServer> cache = proxyCache.get();
            if (cache != null) {
                try {
                    cache.cleanUp();
                } catch (RuntimeException e) {
                    LOG.warn("Error occurred while attempting to clean up expired proxies", e);
                }
            } else {
                // the cache instance was garbage collected, so it no longer needs to be cleaned up. throw an exception
                // to prevent the scheduled executor from re-scheduling this cleanup
                LOG.info("Proxy Cache was garbage collected. No longer cleaning up expired proxies for unused ProxyManager.");

                throw new RuntimeException("Exiting ProxyCleanupTask");
            }
        }
    }

    @Inject
    public ProxyManager(@Named("minPort") Integer minPort, @Named("maxPort") Integer maxPort, final @Named("ttl") Integer ttl) {
        this.minPort = minPort;
        this.maxPort = maxPort;
        this.lastPort = maxPort;
        if (ttl > 0) {
            // proxies should be evicted after the specified ttl, so set up an evicting cache and a listener to stop the proxies when they're evicted
            RemovalListener<Integer, BrowserUpProxyServer> removalListener = new RemovalListener<Integer, BrowserUpProxyServer>() {
                public void onRemoval(RemovalNotification<Integer, BrowserUpProxyServer> removal) {
                    try {
                        BrowserUpProxyServer proxy = removal.getValue();
                        if (proxy != null) {
                            LOG.info("Expiring ProxyServer on port {} after {} seconds without activity", proxy.getPort(), ttl);
                            proxy.stop();
                        }
                    } catch (Exception ex) {
                        LOG.warn("Error while stopping an expired proxy on port " + removal.getKey(), ex);
                    }
                }
            };

            this.proxyCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(ttl, TimeUnit.SECONDS)
                    .removalListener(removalListener)
                    .build();

            this.proxies = proxyCache.asMap();

            // schedule the asynchronous proxy cleanup task
            ScheduledExecutorHolder.expiredProxyCleanupExecutor.scheduleWithFixedDelay(new ProxyCleanupTask(proxyCache),
                    EXPIRED_PROXY_CLEANUP_INTERVAL_SECONDS, EXPIRED_PROXY_CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
        } else {
            this.proxies = new ConcurrentHashMap<Integer, BrowserUpProxyServer>();
            // nothing to timeout, so no Cache
            this.proxyCache = null;
        }
    }

    public BrowserUpProxyServer create(String upstreamHttpProxy, String proxyUsername, String proxyPassword, Integer port, String bindAddr, String serverBindAddr, boolean useEcc, boolean trustAllServers) {
        return create(upstreamHttpProxy, false, null, proxyUsername, proxyPassword, port, bindAddr, serverBindAddr, useEcc, trustAllServers);
    }

    public BrowserUpProxyServer create(String upstreamHttpProxy, boolean upstreamProxyHttps, String proxyUsername, String proxyPassword, Integer port, String bindAddr, String serverBindAddr, boolean useEcc, boolean trustAllServers) {
        return create(upstreamHttpProxy, upstreamProxyHttps, null, proxyUsername, proxyPassword, port, bindAddr, serverBindAddr, useEcc, trustAllServers);
    }

    public BrowserUpProxyServer create(String upstreamHttpProxy, boolean upstreamProxyHttps, List<String> upstreamNonProxyHosts, String proxyUsername, String proxyPassword, Integer port, String bindAddr, String serverBindAddr, boolean useEcc, boolean trustAllServers) {

        LOG.debug("Instantiate ProxyServer...");
        BrowserUpProxyServer proxy = new BrowserUpProxyServer();

        if (useEcc) {
            LOG.info("Using Elliptic Curve Cryptography for certificate impersonation");

            proxy.setUseEcc(true);
        }

        if (trustAllServers) {
            proxy.setTrustAllServers(true);
        }

        // this is a short-term work-around for Proxy Auth in the REST API until the upcoming REST API refactor
        if (proxyUsername != null && proxyPassword != null) {
            proxy.chainedProxyAuthorization(proxyUsername, proxyPassword, AuthType.BASIC);
        }

        if (upstreamHttpProxy != null) {
            try {
                InetSocketAddress chainedProxyAddress = BrowserUpProxyUtil.inetSocketAddressFromString(upstreamHttpProxy);
                proxy.setChainedProxy(chainedProxyAddress);
            } catch (URISyntaxException e) {
                LOG.error("Invalid upstream http proxy specified: " + upstreamHttpProxy + ". Must use host:port format.");
                throw new RuntimeException("Invalid upstream http proxy");
            }

            proxy.setChainedProxyHTTPS(upstreamProxyHttps);

            if (upstreamNonProxyHosts != null) {
                proxy.setChainedProxyNonProxyHosts(upstreamNonProxyHosts);
            }
        }

        InetAddress clientBindAddress = null;
        if (bindAddr != null) {
            LOG.debug("Bind ProxyServer to `{}`...", bindAddr);
            try {
                clientBindAddress = InetAddress.getByName(bindAddr);
            } catch (UnknownHostException e) {
                LOG.error("Unable to bind proxy to address: " + bindAddr + "; proxy will not be created.", e);

                throw new RuntimeException("Unable to bind proxy to address: ", e);
            }
        }

        InetAddress serverInetAddress = null;
        if (serverBindAddr != null) {
            LOG.debug("Bind ProxyServer serverAddress to `{}`...", serverBindAddr);
            try {
                serverInetAddress = InetAddress.getByName(serverBindAddr);
            } catch (UnknownHostException e) {
                LOG.error("Unable to bind proxy to server address: " + serverBindAddr + "; proxy will not be created.", e);

                throw new RuntimeException("Unable to bind proxy to server address: ", e);
            }
        }

        if (port != null) {
            return startProxy(proxy, port, clientBindAddress, serverInetAddress);
        }

        while (proxies.size() <= maxPort - minPort) {
            LOG.debug("Use next available port for new ProxyServer...");
            port = nextPort();
            try {
                return startProxy(proxy, port, clientBindAddress, serverInetAddress);
            } catch (ProxyExistsException ex) {
                LOG.debug("Proxy already exists at port {}", port);
            }
        }
        throw new ProxyPortsExhaustedException();
    }
    
    /**
     * Check that the Port is Available to bind to.
     * 
     * We need to verify that the port isn't already bound to on the client bind address.
     * The application by default assumes that the min-max ports are reserved, however 
     * in a mixed environment there may be some stray ports that are being used by 
     * other services. 
     * 
     * This method will verify that the port isn't already bound by trying 
     * to bind to it and then closing it. 
     * 
     * If the port is already bound it will throw a run-time exception.
     * 
     * @param clientBindAddress the bind address to check, can be null which is a wildcard
     * @param port the port to attempt to bind to with the bind address
     */
    private void checkPortAvailability(InetAddress clientBindAddress, Integer port) {

        InetSocketAddress testClientBindSocket;
        
        // Use the wildcard address if the clientBindAddress isn't specified
        if (clientBindAddress == null) {
        	testClientBindSocket = new InetSocketAddress(port);
        } else {
        	testClientBindSocket = new InetSocketAddress(clientBindAddress, port);
        }
        
        // Use a test socket to attempt binding with
        try(Socket testSocket = new Socket()) {
        	testSocket.bind(testClientBindSocket);
        } catch (IOException e) {
        	// Assume that the port cannot be bound to, log a warning and then return False
            LOG.error("Bind address unavailable: " + testClientBindSocket.toString(), e);
            throw new AddressInUseException(e, port, clientBindAddress);            
		}
        
        // Log that the bind address is available
        LOG.debug("Bind address available {}", testClientBindSocket);
	}

	public BrowserUpProxyServer create(String upstreamHttpProxy, String proxyUsername, String proxyPassword, Integer port, String bindAddr, boolean useEcc, boolean trustAllServers) {
        return create(upstreamHttpProxy, false, null, proxyUsername, proxyPassword, port, null, null, false, false);
    }

    public BrowserUpProxyServer create(String upstreamHttpProxy, String proxyUsername, String proxyPassword, Integer port) {
        return create(upstreamHttpProxy, false, null, proxyUsername, proxyPassword, port, null, null, false, false);
    }

    public BrowserUpProxyServer create(String upstreamHttpProxy, String proxyUsername, String proxyPassword) {
        return create(upstreamHttpProxy, false, null, proxyUsername, proxyPassword, null, null, null, false, false);
    }

    public BrowserUpProxyServer create() {
        return create(null, false, null, null, null, null, null, null, false, false);
    }

    public BrowserUpProxyServer create(int port) {
        return create(null, false, null, null, null, port, null, null, false, false);
    }

    public BrowserUpProxyServer get(int port) {
        return proxies.get(port);
    }

    private BrowserUpProxyServer startProxy(BrowserUpProxyServer proxy, int port, InetAddress clientBindAddr, InetAddress serverBindAddr) {
        if (port != 0) {
            BrowserUpProxyServer old = proxies.putIfAbsent(port, proxy);
            if (old != null) {
                LOG.info("Proxy already exists at port {}", port);
                throw new ProxyExistsException(port);
            }
            checkPortAvailability(clientBindAddr, port);            
        }

        try {
            proxy.start(port, clientBindAddr, serverBindAddr);

            if (port == 0) {
                int realPort = proxy.getPort();
                proxies.put(realPort, proxy);
            }

            return proxy;
        } catch (Exception ex) {
            if (port != 0) {
                proxies.remove(port);
            }
            try {
                proxy.stop();
            } catch (Exception ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    private synchronized int nextPort() {
        return lastPort < maxPort ? ++lastPort : (lastPort = minPort);
    }

    public Collection<BrowserUpProxyServer> get() {
        return proxies.values();
    }

    public void delete(int port) {
        BrowserUpProxyServer proxy = proxies.remove(port);
        if (proxy == null) {
            return;
        }

        // temporary: to avoid stopping an already-stopped BrowserUpProxyServer instance, see if it's stopped before re-stopping it
        if (!(proxy).isStopped()) {
            proxy.stop();
        }
    }

}
