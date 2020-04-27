package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyManager;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.tuple.Pair.of;

public class AdditionalHeadersManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyManager mitmProxyManager;
    private final Map<String, String> headers = new HashMap<>();

    public AdditionalHeadersManager(AddonsManagerClient addonsManagerClient, MitmProxyManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void addHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);

        if (!mitmProxyManager.isRunning()) return;

        List<Pair<String, String>> params = headers.entrySet()
                .stream()
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        addonsManagerClient.
                getRequestToAddonsManager(
                        "additional_headers",
                        "add_headers",
                        params,
                        Void.class);
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "additional_headers",
                        "add_header",
                        new ArrayList<Pair<String, String>>() {{
                            add(of(name, valueOf(value)));
                        }},
                        Void.class);
    }

    public void removeHeader(String name) {
        headers.remove(name);

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "additional_headers",
                        "remove_header",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("name", valueOf(name)));
                        }},
                        Void.class);
    }

    public void removeAllHeaders() {
        headers.clear();

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "additional_headers",
                        "remove_all_headers",
                        new ArrayList<>(),
                        Void.class);
    }

    public Map<String, String> getAllHeaders() {
        return ImmutableMap.copyOf(headers);
    }

}
