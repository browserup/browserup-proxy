package net.lightbody.bmp.proxy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class ProxyManager {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyManager.class);

    private int lastPort;
    private final int minPort; 
    private final int maxPort;
    private final Provider<ProxyServer> proxyServerProvider;
    private final ConcurrentMap<Integer, ProxyServer> proxies;

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
                            LOG.debug("Expiring ProxyServer on port {} after {} seconds without activity", proxy.getPort(), ttl);
                            proxy.stop();
                        }
                    } catch (Exception ex) {
                        LOG.warn("Error while stopping an expired proxy on port " + removal.getKey(), ex);
                    }
                }
            };

            this.proxies = CacheBuilder.newBuilder()
                    .expireAfterAccess(ttl, TimeUnit.SECONDS)
                    .removalListener(removalListener)
                    .build()
                    .asMap();
        } else {
            this.proxies = new ConcurrentHashMap<Integer, ProxyServer>();
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
