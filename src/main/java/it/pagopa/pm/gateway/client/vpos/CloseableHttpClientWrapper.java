package it.pagopa.pm.gateway.client.vpos;


import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
class CloseableHttpClientWrapper {

    private static final String MODULE_NAME = "pp-vpos-client";

    private final CloseableHttpClient client;

    CloseableHttpClientWrapper(CloseableHttpClient client) {
        this.client = client;
    }

    CloseableHttpResponse execute(final HttpUriRequest request) throws IOException {
        Instant start = Instant.now();
        try {
            return client.execute(request);
        } finally {
            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();

            log.info(createLogginElapsedString( request.getURI().toString(), timeElapsed));
        }
    }

    void close() throws IOException {
        client.close();
    }

    private static String createLogginElapsedString(String serviceName, long timeElapsed) {
        return String.format("elapsed|%s|%s:%s", MODULE_NAME, serviceName, timeElapsed);
    }

}

