package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import com.browserup.bup.proxy.BlacklistEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.tuple.Pair.of;

public class BlackListManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyProcessManager mitmProxyManager;

    public BlackListManager(AddonsManagerClient addonsManagerClient, MitmProxyProcessManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void blacklistRequests(String urlPattern, int statusCode) {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "blacklist",
                        "blacklist_requests",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("urlPattern", valueOf(urlPattern)));
                            add(of("statusCode", valueOf(statusCode)));
                        }},
                        Void.class);
    }

    public void blacklistRequests(String urlPattern, int statusCode, String httpMethodPattern) {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "blacklist",
                        "blacklist_requests",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("urlPattern", valueOf(urlPattern)));
                            add(of("statusCode", valueOf(statusCode)));
                            add(of("httpMethodPattern", valueOf(httpMethodPattern)));
                        }},
                        Void.class);
    }

    public void setBlacklist(Collection<BlacklistEntry> blacklist) {
        if (!mitmProxyManager.isRunning()) return;

        String serializedBlackList;
        try {
            serializedBlackList = new ObjectMapper().writeValueAsString(blacklist);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Couldn't serialize black list", e);
        }
        addonsManagerClient.
                putRequestToAddonsManager(
                        "blacklist",
                        "set_black_list",
                        Collections.emptyList(),
                        RequestBody.create(serializedBlackList, MediaType.parse("application/json; charset=utf-8")),
                        Void.class);
    }
}
