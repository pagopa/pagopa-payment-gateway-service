package it.pagopa.pm.gateway.client.vpos;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


@Slf4j
@Component
public class HttpClient {

    private static final String VPOS_TIMEOUT_STRING = "VPOS_TIMEOUT";
    private static final int DEFAULT_VALUE_TIMEMOUT = 20000;
    private static final int VPOS_TIMEOUT = getTimeout();
    private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

    private final Map<String, String> requestHeaders = new HashMap<>();

    public HttpClientResponse post(String url, String contentType, Map<String, String> params) throws IOException {
        String trace = String.format("POST - %s", url);
        log.info("INIT " + trace);
        CloseableHttpClientWrapper client = createClient();
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            HttpPost request = new HttpPost(uriBuilder.build());
            if (contentType != null) {
                request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
            }
            if (contentType != null) {
                request.setHeader(HttpHeaders.CONTENT_TYPE, requestHeaders.getOrDefault(HttpHeaders.CONTENT_TYPE, contentType));
            }
            if (params != null) {
                List<NameValuePair> values = new ArrayList<>();
                for (Entry<String, String> entry : params.entrySet()) {
                    NameValuePair value = new BasicNameValuePair(entry.getKey(), entry.getValue());
                    values.add(value);
                }
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(values,
                        DEFAULT_CHARSET.displayName());
                request.setEntity(entity);
            }
            HttpResponse response = client.execute(request);
            return initResponse(response);
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            throw ioe;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException(e);
        } finally {
            client.close();
            log.info("END " + trace);
        }
    }

    private HttpClientResponse initResponse(HttpResponse httpResponse) throws IOException {
        HttpClientResponse result = new HttpClientResponse();
        result.setStatus(httpResponse.getStatusLine().getStatusCode());
        byte[] entity = IOUtils.toByteArray(httpResponse.getEntity().getContent());
        result.setEntity(entity);
        return result;
    }

    private CloseableHttpClientWrapper createClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(VPOS_TIMEOUT)
                .setConnectionRequestTimeout(VPOS_TIMEOUT)
                .setSocketTimeout(VPOS_TIMEOUT).build();

        return new CloseableHttpClientWrapper(HttpClientBuilder.create().useSystemProperties().setDefaultRequestConfig(config).build());
    }

    private static int getTimeout() {
        int integer = DEFAULT_VALUE_TIMEMOUT;
        String customTimeout = System.getProperty(VPOS_TIMEOUT_STRING);
        if (StringUtils.isNotBlank(customTimeout) && NumberUtils.isParsable(customTimeout)) {
            integer = Integer.parseInt(customTimeout);
            log.debug(String.format("Set custom timeout to %s for %s", customTimeout, VPOS_TIMEOUT_STRING));
        } else {
            log.debug(String.format("Custom timeout not found for %s. Use default value", VPOS_TIMEOUT_STRING));
        }
        log.info(String.format("Timeout set to %s for %s", integer, VPOS_TIMEOUT_STRING));
        return integer;
    }
}

