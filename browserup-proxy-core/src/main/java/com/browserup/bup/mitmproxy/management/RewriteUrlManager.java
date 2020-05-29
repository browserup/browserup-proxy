package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import com.browserup.bup.proxy.RewriteRule;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.tuple.Pair.of;

public class RewriteUrlManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyProcessManager mitmProxyManager;

    private volatile CopyOnWriteArrayList<RewriteRule> rewriteRules = new CopyOnWriteArrayList<>();

    public RewriteUrlManager(AddonsManagerClient addonsManagerClient, MitmProxyProcessManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void rewriteUrl(String pattern, String replace) {
        rewriteRules.add(new RewriteRule(pattern, replace));

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "rewrite_url",
                        "rewrite_url",
                        new ArrayList<Pair<String, String>>() {{
                            add(of(pattern, valueOf(replace)));
                        }},
                        Void.class);
    }

    public void rewriteUrls(Map<String, String> rewriteRules) {
        this.rewriteRules = rewriteRules.entrySet().stream()
                .map(rewriteRule -> new RewriteRule(rewriteRule.getKey(), rewriteRule.getValue()))
                .collect(toCollection(CopyOnWriteArrayList::new));

        if (!mitmProxyManager.isRunning()) return;

        List<Pair<String, String>> params = rewriteRules.entrySet()
                .stream()
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        addonsManagerClient.
                getRequestToAddonsManager(
                        "rewrite_url",
                        "rewrite_urls",
                        params,
                        Void.class);
    }

    public void removeRewriteRule(String urlPattern) {
        rewriteRules.stream()
                .filter(rewriteRule -> rewriteRule.getPattern().pattern().equals(urlPattern))
                .forEach(rewriteRule -> rewriteRules.remove(rewriteRule));

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "rewrite_url",
                        "remove_rewrite_rule",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("pattern", valueOf(urlPattern)));
                        }},
                        Void.class);
    }

    public void clearRewriteRules() {
        rewriteRules.clear();

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "rewrite_url",
                        "clear_rewrite_rules",
                        Collections.emptyList(),
                        Void.class);
    }

    public CopyOnWriteArrayList<RewriteRule> getRewriteRules() {
        return rewriteRules;
    }

    public Map<String, String> getRewriteRulesMap() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        rewriteRules.forEach(rewriteRule -> builder.put(rewriteRule.getPattern().pattern(), rewriteRule.getReplace()));

        return builder.build();
    }
}
