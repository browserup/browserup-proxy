package com.browserup.bup.mitmproxy.management;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

public class HarCaptureFilterManager {
    private final AddonsManagerClient addonsManagerClient;

    public HarCaptureFilterManager(AddonsManagerClient addonsManagerClient) {
        this.addonsManagerClient = addonsManagerClient;
    }

    public String getCurrentHarFilePath(Boolean cleanHar) {
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
