package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyManager;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.tuple.Pair.of;

public class LatencyManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyManager mitmProxyManager;

    private volatile int latencyMs;

    public LatencyManager(AddonsManagerClient addonsManagerClient, MitmProxyManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void setLatency(long latency, TimeUnit timeUnit) {
        this.latencyMs = (int) TimeUnit.MILLISECONDS.convert(latency, timeUnit);

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "latency",
                        "set_latency",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("latency", valueOf(latencyMs)));
                        }},
                        Void.class);
    }

    public int getLatencyMs() {
        return latencyMs;
    }
}
