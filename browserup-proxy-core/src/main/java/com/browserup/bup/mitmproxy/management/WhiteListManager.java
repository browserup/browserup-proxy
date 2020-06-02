package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import com.browserup.bup.proxy.Whitelist;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.tuple.Pair.of;

public class WhiteListManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyProcessManager mitmProxyManager;

    private final AtomicReference<Whitelist> whitelist = new AtomicReference<>(Whitelist.WHITELIST_DISABLED);

    public WhiteListManager(AddonsManagerClient addonsManagerClient, MitmProxyProcessManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void whitelistRequests(Collection<String> urlPatterns, int statusCode) {
        this.whitelist.set(new Whitelist(urlPatterns, statusCode));

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
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

        boolean whitelistUpdated = false;
        while (!whitelistUpdated) {
            Whitelist currentWhitelist = this.whitelist.get();
            if (!currentWhitelist.isEnabled()) {
                throw new IllegalStateException("Whitelist is disabled. Cannot add patterns to a disabled whitelist.");
            }
            int statusCode = currentWhitelist.getStatusCode();
            List<String> newPatterns = currentWhitelist.getPatterns().stream()
                    .map(Pattern::pattern)
                    .collect(toCollection(() -> new ArrayList<>(currentWhitelist.getPatterns().size() + 1)));
            newPatterns.add(urlPattern);

            Whitelist newWhitelist = new Whitelist(newPatterns, statusCode);
            whitelistUpdated = this.whitelist.compareAndSet(currentWhitelist, newWhitelist);

            addonsManagerClient.
                    getRequestToAddonsManager(
                            "whitelist",
                            "add_whitelist_pattern",
                            new ArrayList<Pair<String, String>>() {{
                                add(of("urlPattern", valueOf(urlPattern)));
                            }},
                            Void.class);
        }
    }

    public void enableEmptyWhitelist(int statusCode) {
        if (!mitmProxyManager.isRunning()) return;

        whitelist.set(new Whitelist(statusCode));

        addonsManagerClient.
                getRequestToAddonsManager(
                        "whitelist",
                        "enable_empty_whitelist",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("statusCode", valueOf(statusCode)));
                        }},
                        Void.class);
    }

    public void disableWhitelist() {
        if (!mitmProxyManager.isRunning()) return;

        whitelist.set(Whitelist.WHITELIST_DISABLED);

        addonsManagerClient.
                getRequestToAddonsManager(
                        "whitelist",
                        "disable_whitelist",
                        new ArrayList<>(),
                        Void.class);
    }

    public int getWhitelistStatusCode() {
        return whitelist.get().getStatusCode();
    }

    public boolean isWhitelistEnabled() {
        return whitelist.get().isEnabled();
    }

    public Collection<String> getWhitelistUrls() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        whitelist.get().getPatterns().stream()
                .map(Pattern::pattern)
                .forEach(builder::add);

        return builder.build();
    }
}
