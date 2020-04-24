package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyManager;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.tuple.Pair.of;

public class AuthBasicFilterManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyManager mitmProxyManager;
    private final Map<String, String> credentials = new HashMap<>();

    public AuthBasicFilterManager(AddonsManagerClient addonsManagerClient, MitmProxyManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void authAuthorization(String domain, String base64EncodedCredentials) {
        credentials.put(domain, base64EncodedCredentials);

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "auth_basic",
                        "auth_authorization",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("domain", valueOf(domain)));
                            add(of("base64EncodedCredentials", valueOf(base64EncodedCredentials)));
                        }},
                        Void.class);
    }

    public void stopAutoAuthorization(String domain) {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "auth_basic",
                        "stop_authorization",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("domain", valueOf(domain)));
                        }},
                        Void.class);
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }
}
