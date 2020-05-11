package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyManager;
import com.browserup.harreader.model.Har;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.tuple.Pair.of;

public class ProxyManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyManager mitmProxyManager;

    private long connectionIdleTimeoutSeconds = -1;

    public ProxyManager(AddonsManagerClient addonsManagerClient, MitmProxyManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void setTrustAll(Boolean trustAll) {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "proxy_manager",
                        "trust_all",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("trustAll", valueOf(trustAll)));
                        }},
                        Void.class);
    }

    public void setConnectionIdleTimeout(Long idleTimeoutSeconds) {
        this.connectionIdleTimeoutSeconds = idleTimeoutSeconds;

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "proxy_manager",
                        "set_connection_timeout_idle",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("idleSeconds", valueOf(idleTimeoutSeconds)));
                        }},
                        Void.class);
    }

    public long getConnectionIdleTimeoutSeconds() {
        return connectionIdleTimeoutSeconds;
    }
}
