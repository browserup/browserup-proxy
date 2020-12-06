package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import com.browserup.bup.proxy.BlocklistEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.tuple.Pair.of;

public class BlockListManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyProcessManager mitmProxyManager;

    private volatile Collection<BlocklistEntry> blocklistEntries = new CopyOnWriteArrayList<>();


    public BlockListManager(AddonsManagerClient addonsManagerClient, MitmProxyProcessManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void blocklistRequests(String urlPattern, int statusCode) {
        if (!mitmProxyManager.isRunning()) return;

        blocklistEntries.add(new BlocklistEntry(urlPattern, statusCode));

        addonsManagerClient.
                getRequestToAddonsManager(
                        "blocklist",
                        "blocklist_requests",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("urlPattern", valueOf(urlPattern)));
                            add(of("statusCode", valueOf(statusCode)));
                        }},
                        Void.class);
    }

    public void blocklistRequests(String urlPattern, int statusCode, String httpMethodPattern) {
        if (!mitmProxyManager.isRunning()) return;

        blocklistEntries.add(new BlocklistEntry(urlPattern, statusCode, httpMethodPattern));

        addonsManagerClient.
                getRequestToAddonsManager(
                        "blocklist",
                        "blocklist_requests",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("urlPattern", valueOf(urlPattern)));
                            add(of("statusCode", valueOf(statusCode)));
                            add(of("httpMethodPattern", valueOf(httpMethodPattern)));
                        }},
                        Void.class);
    }

    public void setBlocklist(Collection<BlocklistEntry> blocklist) {
        if (!mitmProxyManager.isRunning()) return;

        this.blocklistEntries = new CopyOnWriteArrayList<>(blocklist);

        String serializedBlockList;
        try {
            serializedBlockList = new ObjectMapper().writeValueAsString(blocklist);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Couldn't serialize block list", e);
        }
        addonsManagerClient.
                putRequestToAddonsManager(
                        "blocklist",
                        "set_block_list",
                        Collections.emptyList(),
                        RequestBody.create(serializedBlockList, MediaType.parse("application/json; charset=utf-8")),
                        Void.class);
    }

    public Collection<BlocklistEntry> getBlocklist() {
        return Collections.unmodifiableCollection(blocklistEntries);
    }

    public void clearBlockList() {
        if (!mitmProxyManager.isRunning()) return;

        blocklistEntries.clear();

        this.setBlocklist(Collections.emptyList());
    }

}
