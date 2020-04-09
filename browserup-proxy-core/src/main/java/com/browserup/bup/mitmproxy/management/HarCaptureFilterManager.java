package com.browserup.bup.mitmproxy.management;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;

public class HarCaptureFilterManager {
    private final AddonsManagerClient addonsManagerClient;

    public HarCaptureFilterManager(AddonsManagerClient addonsManagerClient) {
        this.addonsManagerClient = addonsManagerClient;
    }

    public String getHar(Boolean cleanHar) {
        CurrentHarResponse response = addonsManagerClient.
                requestToAddonsManager(
                        "har",
                        "get_har",
                        new ArrayList<Pair<String, String>>() {{
                            add(Pair.of("cleanHar", String.valueOf(cleanHar)));
                        }},
                        CurrentHarResponse.class);
        return response.path;
    }

    public String newHar(String pageRef, String pageTitle) {
        CurrentHarResponse response = addonsManagerClient.
                requestToAddonsManager(
                        "har",
                        "new_har",
                        new ArrayList<Pair<String, String>>() {{
                            add(Pair.of("pageRef", String.valueOf(pageRef)));
                            add(Pair.of("pageTitle", String.valueOf(pageTitle)));
                        }},
                        CurrentHarResponse.class);
        return response.path;
    }

    public String endHar() {
        CurrentHarResponse response = addonsManagerClient.
                requestToAddonsManager(
                        "har",
                        "end_har",
                        Collections.emptyList(),
                        CurrentHarResponse.class);
        return response.path;
    }

    public String newPage(String pageRef, String pageTitle) {
        CurrentHarResponse response = addonsManagerClient.
                requestToAddonsManager(
                        "har",
                        "new_page",
                        new ArrayList<Pair<String, String>>() {{
                            add(Pair.of("pageRef", String.valueOf(pageRef)));
                            add(Pair.of("pageTitle", String.valueOf(pageTitle)));
                        }},
                        CurrentHarResponse.class);
        return response.path;
    }

    public void endPage() {
        addonsManagerClient.
                requestToAddonsManager(
                        "har",
                        "end_page",
                        Collections.emptyList(),
                        Void.class);
    }

    public static class CurrentHarResponse {
        private String path;

        public CurrentHarResponse() {}

        public CurrentHarResponse(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
