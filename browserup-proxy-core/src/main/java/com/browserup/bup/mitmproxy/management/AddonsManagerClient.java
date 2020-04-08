package com.browserup.bup.mitmproxy.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AddonsManagerClient {
    private final int port;
    private final String host = "localhost";

    public AddonsManagerClient(int port) {
        this.port = port;
    }

    public <T> T requestToAddonsManager(String addOnPath,
                                        String operation,
                                        Class<T> responseClass) {
        return requestToAddonsManager(addOnPath, operation, Collections.emptyList(), responseClass);
    }

    public <T> T requestToAddonsManager(String addOnPath,
                                        String operation,
                                        List<Pair<String, String>> queryParams,
                                        Class<T> responseClass) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(buildRequestUrl(addOnPath, operation, queryParams))
                .get()
                .build();

        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to request manager API", ex);
        }

        try {
            return new ObjectMapper().readerFor(responseClass).readValue(Objects.requireNonNull(response.body()).byteStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse response from manager API", e);
        }
    }

    private HttpUrl buildRequestUrl(String addOnPath, String operation, List<Pair<String, String>> queryParams) {
        HttpUrl.Builder builder = new HttpUrl.Builder()
                .host(host)
                .port(port)
                .scheme("http")
                .addPathSegment(addOnPath)
                .addPathSegment(operation);

        queryParams.forEach(p -> {
            builder.addQueryParameter(p.getKey(), p.getValue());
        });

        return builder.build();
    }
}
