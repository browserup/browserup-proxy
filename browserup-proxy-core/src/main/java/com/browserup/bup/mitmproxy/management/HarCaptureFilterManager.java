package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyManager;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.harreader.model.Har;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.tuple.Pair.of;

public class HarCaptureFilterManager {
    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyManager mitmProxyManager;
    private Har lastHar = new Har();
    private EnumSet<CaptureType> lastCaptureTypes = EnumSet.noneOf(CaptureType.class);

    public HarCaptureFilterManager(AddonsManagerClient addonsManagerClient, MitmProxyManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public Har getHar() {
        return getHar(false);
    }

    public Har getHar(Boolean cleanHar) {
        if (!mitmProxyManager.isRunning()) return lastHar;

        HarResponse response = addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "get_har",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("cleanHar", valueOf(cleanHar)));
                        }},
                        HarResponse.class);
        System.out.println("PARSING HAR: " + response.path);
        return parseHar(response.path);
    }

    public Har newHar() {
        return newHar(null, null);
    }

    public Har newHar(String pageRef) {
        return newHar(pageRef, null);
    }

    public Har newHar(String pageRef, String pageTitle) {
        if (!mitmProxyManager.isRunning()) return lastHar;

        HarResponse response = addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "new_har",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("pageRef", pageRef));
                            add(of("pageTitle", pageTitle));
                        }},
                        HarResponse.class);
        return parseHar(response.path);
    }

    public Har endHar() {
        if (!mitmProxyManager.isRunning()) return lastHar;

        HarResponse response = addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "end_har",
                        emptyList(),
                        HarResponse.class);
        return parseHar(response.path);
    }

    public Har newPage() {
        return newPage(null, null);
    }

    public Har newPage(String pageRef) {
        return newPage(pageRef, null);
    }

    public Har newPage(String pageRef, String pageTitle) {
        if (!mitmProxyManager.isRunning()) return lastHar;

        HarResponse response = addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "new_page",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("pageRef", valueOf(pageRef)));
                            add(of("pageTitle", valueOf(pageTitle)));
                        }},
                        HarResponse.class);
        return parseHar(response.path);
    }

    public void endPage() {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "end_page",
                        emptyList(),
                        Void.class);
    }

    private Har parseHar(String filePath) {
        File harFile = new File(filePath);

        Har har;
        try {
            har = new ObjectMapper().readerFor(Har.class).readValue(harFile);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read HAR file: " + harFile.getAbsolutePath(), e);
        }
        lastHar = har;
        return har;
    }

    public void setHarCaptureTypes(EnumSet<CaptureType> captureTypes) {
        lastCaptureTypes = captureTypes;

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "set_har_capture_types",
                        new ArrayList<Pair<String, String>>() {{
                            add(of("captureTypes", valueOf(captureTypes)));
                        }},
                        Void.class);
    }

    public EnumSet<CaptureType> getLastCaptureTypes() {
        return lastCaptureTypes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HarResponse {
        private String path;

        public HarResponse() {}

        public HarResponse(String path) {
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
