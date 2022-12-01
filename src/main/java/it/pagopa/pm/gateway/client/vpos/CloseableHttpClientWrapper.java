package it.pagopa.pm.gateway.client.vpos;


import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
class CloseableHttpClientWrapper implements AutoCloseable {
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
            log.info(String.format("PGS-VPos-client: %s ms elapsed for request %s", timeElapsed, request.getURI().toString()));
        }
    }

    @Override
    public void close() throws Exception {
        client.close();
    }


}

