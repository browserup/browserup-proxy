package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import com.browserup.bup.proxy.Allowlist;
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

public class AllowListManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyProcessManager mitmProxyManager;

    private final AtomicReference<Allowlist> allowlist = new AtomicReference<>(Allowlist.ALLOWLIST_DISABLED);

    public AllowListManager(AddonsManagerClient addonsManagerClient, MitmProxyProcessManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void allowlistRequests(Collection<String> urlPatterns, int statusCode) {
        this.allowlist.set(new Allowlist(urlPatterns, statusCode));

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "allowlist",
                        "allowlist_requests",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("urlPatterns", valueOf(urlPatterns)));
                            add(of("statusCode", valueOf(statusCode)));
                        }},
                        Void.class);
    }

    public void addAllowlistPattern(String urlPattern) {
        if (!mitmProxyManager.isRunning()) return;

        boolean allowlistUpdated = false;
        while (!allowlistUpdated) {
            Allowlist currentAllowlist = this.allowlist.get();
            if (!currentAllowlist.isEnabled()) {
                throw new IllegalStateException("Allowlist is disabled. Cannot add patterns to a disabled allowlist.");
            }
            int statusCode = currentAllowlist.getStatusCode();
            List<String> newPatterns = currentAllowlist.getPatterns().stream()
                    .map(Pattern::pattern)
                    .collect(toCollection(() -> new ArrayList<>(currentAllowlist.getPatterns().size() + 1)));
            newPatterns.add(urlPattern);

            Allowlist newAllowlist = new Allowlist(newPatterns, statusCode);
            allowlistUpdated = this.allowlist.compareAndSet(currentAllowlist, newAllowlist);

            addonsManagerClient.
                    getRequestToAddonsManager(
                            "allowlist",
                            "add_allowlist_pattern",
                            new ArrayList<Pair<String, String>>() {{
                                add(of("urlPattern", valueOf(urlPattern)));
                            }},
                            Void.class);
        }
    }

    public void enableEmptyAllowlist(int statusCode) {
        if (!mitmProxyManager.isRunning()) return;

        allowlist.set(new Allowlist(statusCode));

        addonsManagerClient.
                getRequestToAddonsManager(
                        "allowlist",
                        "enable_empty_allowlist",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("statusCode", valueOf(statusCode)));
                        }},
                        Void.class);
    }

    public void disableAllowlist() {
        if (!mitmProxyManager.isRunning()) return;

        allowlist.set(Allowlist.ALLOWLIST_DISABLED);

        addonsManagerClient.
                getRequestToAddonsManager(
                        "allowlist",
                        "disable_allowlist",
                        new ArrayList<>(),
                        Void.class);
    }

    public int getAllowlistStatusCode() {
        return allowlist.get().getStatusCode();
    }

    public boolean isAllowlistEnabled() {
        return allowlist.get().isEnabled();
    }

    public Collection<String> getAllowlistUrls() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        allowlist.get().getPatterns().stream()
                .map(Pattern::pattern)
                .forEach(builder::add);

        return builder.build();
    }
}
