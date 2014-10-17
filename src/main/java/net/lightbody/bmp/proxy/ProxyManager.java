package net.lightbody.bmp.proxy;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.lightbody.bmp.proxy.util.Log;

@Singleton
public class ProxyManager {
    private static final Log LOG = new Log();

    private int lastPort;
    private final int minPort; 
    private final int maxPort;
    private Provider<ProxyServer> proxyServerProvider;
    private ConcurrentHashMap<Integer, ProxyServer> proxies = new ConcurrentHashMap<Integer, ProxyServer>();

    @Inject
    public ProxyManager(Provider<ProxyServer> proxyServerProvider, @Named("minPort") Integer minPort, @Named("maxPort") Integer maxPort) {
        this.proxyServerProvider = proxyServerProvider;
        this.minPort = minPort;
        this.maxPort = maxPort;
        this.lastPort = maxPort; 
    }

    public ProxyServer create(Map<String, String> options, Integer port, String bindAddr) throws Exception {
        LOG.fine("Instantiate ProxyServer...");
        ProxyServer proxy = proxyServerProvider.get();
        
        LOG.fine("Apply options `{}` to new ProxyServer...", options);
        proxy.setOptions(options);                        
        
        if (bindAddr != null) {            
            LOG.fine("Bind ProxyServer to `{}`...", bindAddr);
            proxy.setLocalHost(InetAddress.getByName(bindAddr));
        }
        
        if (port != null) {                        
            return startProxy(proxy, port);            
        }                
        
        while(proxies.size() <= maxPort-minPort){
            LOG.fine("Use next available port for new ProxyServer...");
            port = nextPort();                        
            try{
                return startProxy(proxy, port);                
            }catch(ProxyExistsException ex){
                LOG.fine("Proxy already exists at port {}", port);
            }
        }
        throw new ProxyPortsExhaustedException();    
    }

    public ProxyServer create(Map<String, String> options, Integer port) throws Exception {
        return create(options, port, null);
    }

    public ProxyServer create(Map<String, String> options) throws Exception {
        return create(options, null, null);
    }

    public ProxyServer get(int port) {
        return proxies.get(port);
    }
    
    private ProxyServer startProxy(ProxyServer proxy, int port) throws Exception{
        proxy.setPort(port);
        ProxyServer old = proxies.putIfAbsent(port, proxy);
        if(old != null){
            LOG.fine("Proxy already exists at port {}", port);
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
    
    public void delete(int port) throws Exception {
        ProxyServer proxy = proxies.remove(port);
        proxy.stop();
    }
}
