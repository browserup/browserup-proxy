package net.lightbody.bmp.proxy;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProxyManager {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyManager.class);

    private final AtomicInteger portCounter = new AtomicInteger(9090);
    private final Provider<ProxyServer> proxyServerProvider;
    private final Map<Integer, ProxyServer> proxies = new ConcurrentHashMap<Integer, ProxyServer>();

    @Inject
    public ProxyManager(Provider<ProxyServer> proxyServerProvider) {
        this.proxyServerProvider = proxyServerProvider;
    }

    public ProxyServer create(Map<String, String> options, Integer port, String bindAddr) {
        LOG.trace("Instantiate ProxyServer...");
        ProxyServer proxy = proxyServerProvider.get();

        if (bindAddr != null) {
            LOG.trace("Bind ProxyServer to `{}`...", bindAddr);
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
            proxy.setPort(port);
        } else {
            LOG.trace("Use next available port for new ProxyServer...");
            proxy.setPort(portCounter.incrementAndGet());
        }

        proxy.start();
        LOG.trace("Apply options `{}` to new ProxyServer...", options);
        proxy.setOptions(options);
        proxies.put(proxy.getPort(), proxy);
        return proxy;
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

    public Collection<ProxyServer> get() {
        return proxies.values();
    }

    public void delete(int port) {
        ProxyServer proxy = proxies.remove(port);
        proxy.stop();
    }
}
