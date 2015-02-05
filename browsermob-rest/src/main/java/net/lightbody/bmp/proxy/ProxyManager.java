package net.lightbody.bmp.proxy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
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
    private final Provider<ProxyServer> proxyServerProvider;
    // retain a reference to the Cache to allow the ProxyCleanupTask to .cleanUp(), since asMap() is just a view into the cache.
    // it would seem to make sense to pass the newly-built Cache directly to the ProxyCleanupTask and have it retain a WeakReference to it, and
    // only maintain a reference to the .asMap() result in this class. puzzlingly, however, the Cache can actually get garbage collected
    // before the .asMap() view of it does.
    private final Cache<Integer, ProxyServer> proxyCache;
    private final ConcurrentMap<Integer, ProxyServer> proxies;

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
        private final WeakReference<Cache<Integer, ProxyServer>> proxyCache;

        public ProxyCleanupTask(Cache<Integer, ProxyServer> cache) {
            this.proxyCache = new WeakReference<Cache<Integer, ProxyServer>>(cache);
        }

        @Override
        public void run() {
            Cache<Integer, ProxyServer> cache = proxyCache.get();
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
    public ProxyManager(Provider<ProxyServer> proxyServerProvider, @Named("minPort") Integer minPort, @Named("maxPort") Integer maxPort, final @Named("ttl") Integer ttl) {
        this.proxyServerProvider = proxyServerProvider;
        this.minPort = minPort;
        this.maxPort = maxPort;
        this.lastPort = maxPort;
        if (ttl > 0) {
            // proxies should be evicted after the specified ttl, so set up an evicting cache and a listener to stop the proxies when they're evicted
            RemovalListener<Integer, ProxyServer> removalListener = new RemovalListener<Integer, ProxyServer> () {
                public void onRemoval(RemovalNotification<Integer, ProxyServer> removal) {
                    try {
                        ProxyServer proxy = removal.getValue();
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
            this.proxies = new ConcurrentHashMap<Integer, ProxyServer>();
            // nothing to timeout, so no Cache
            this.proxyCache = null;
        }
    }

    public ProxyServer create(Map<String, String> options, Integer port, String bindAddr) {
        LOG.debug("Instantiate ProxyServer...");
        ProxyServer proxy = proxyServerProvider.get();
        
        LOG.debug("Apply options `{}` to new ProxyServer...", options);
        proxy.setOptions(options);                        
        
        if (bindAddr != null) {            
            LOG.debug("Bind ProxyServer to `{}`...", bindAddr);
            InetAddress inetAddress;
            try {
            	inetAddress = InetAddress.getByName(bindAddr);
            } catch (UnknownHostException e) {
            	LOG.error("Unable to bind proxy to address: " + bindAddr + "; proxy will not be created.", e);
            	
            	throw new RuntimeException("Unable to bind proxy to address: ", e);
            }
            
            proxy.setLocalHost(inetAddress);
        }
        
        if (port != null) {                        
            return startProxy(proxy, port);            
        }                
        
        while(proxies.size() <= maxPort-minPort){
            LOG.debug("Use next available port for new ProxyServer...");
            port = nextPort();                        
            try{
                return startProxy(proxy, port);                
            }catch(ProxyExistsException ex){
                LOG.debug("Proxy already exists at port {}", port);
            }
        }
        throw new ProxyPortsExhaustedException();    
    }

    public ProxyServer create(Map<String, String> options, Integer port) {
        return create(options, port, null);
    }

    public ProxyServer create(Map<String, String> options) {
        return create(options, null, null);
    }

    public ProxyServer get(int port) {
        return proxies.get(port);
    }
    
    private ProxyServer startProxy(ProxyServer proxy, int port) {
        proxy.setPort(port);
        ProxyServer old = proxies.putIfAbsent(port, proxy);
        if(old != null){
            LOG.info("Proxy already exists at port {}", port);
            throw new ProxyExistsException(port);
        }
        try{
            proxy.start();
            return proxy;
        }catch(Exception ex){
            proxies.remove(port);
            try{
                proxy.stop();
            }catch(Exception ex2){
                ex.addSuppressed(ex2);
            }                
            throw ex;
        }
    }
    
    private synchronized int nextPort(){
        return lastPort < maxPort? ++lastPort : (lastPort = minPort); 
    }

    public Collection<ProxyServer> get() {
        return proxies.values();
    }

    public void delete(int port) {
        ProxyServer proxy = proxies.remove(port);
        proxy.stop();
    }
    
}
