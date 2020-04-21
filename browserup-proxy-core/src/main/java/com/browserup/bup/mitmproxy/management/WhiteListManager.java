package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyManager;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.tuple.Pair.of;

public class WhiteListManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyManager mitmProxyManager;

    public WhiteListManager(AddonsManagerClient addonsManagerClient, MitmProxyManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void whitelistRequests(Collection<String> urlPatterns, int statusCode) {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                requestToAddonsManager(
                        "whitelist",
                        "whitelist_requests",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("urlPatterns", valueOf(urlPatterns)));
                            add(of("statusCode", valueOf(statusCode)));
                        }},
                        Void.class);
    }

    public void addWhitelistPattern(String urlPattern) {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                requestToAddonsManager(
                        "whitelist",
                        "add_whitelist_pattern",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("urlPattern", valueOf(urlPattern)));
                        }},
                        Void.class);
    }

    public void enableEmptyWhitelist(int statusCode) {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                requestToAddonsManager(
                        "whitelist",
                        "enable_empty_whitelist",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("statusCode", valueOf(statusCode)));
                        }},
                        Void.class);
    }

    public void disableWhitelist() {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                requestToAddonsManager(
                        "whitelist",
                        "disable_whitelist",
                        new ArrayList<>(),
                        Void.class);
    }
}
