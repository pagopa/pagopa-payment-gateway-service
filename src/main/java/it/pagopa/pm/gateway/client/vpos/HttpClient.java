package it.pagopa.pm.gateway.client.vpos;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class HttpClient {

    private static final String VPOS_TIMEOUT_STRING = "VPOS_TIMEOUT";
    private static final int DEFAULT_VALUE_TIMEOUT = 20000;
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
            addContentType(contentType, request);
            addRequestHeaders(contentType, request);
            addRequestParams(params, request);
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

    private void addContentType(String contentType, HttpPost request) {
        if (StringUtils.isNotBlank(contentType)) {
            request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
        }
    }

    private void addRequestHeaders(String contentType, HttpPost request) {
        if (ObjectUtils.isNotEmpty(requestHeaders) && StringUtils.isNotBlank(contentType)) {
            String headerValue = requestHeaders.getOrDefault(HttpHeaders.CONTENT_TYPE, contentType);
            request.setHeader(HttpHeaders.CONTENT_TYPE, headerValue);
        }
    }

    private void addRequestParams(Map<String, String> params, HttpPost request) throws UnsupportedEncodingException {
        if (ObjectUtils.isNotEmpty(params)) {
            List<NameValuePair> values = params.entrySet().stream()
                    .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(values, DEFAULT_CHARSET.displayName());
            request.setEntity(entity);
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
        return new CloseableHttpClientWrapper(
                HttpClientBuilder.create()
                        .useSystemProperties()
                        .setDefaultRequestConfig(config)
                        .build()
        );
    }

    private static int getTimeout() {
        int integer = DEFAULT_VALUE_TIMEOUT;
        String customTimeout = System.getProperty(VPOS_TIMEOUT_STRING);
        if (StringUtils.isNotBlank(customTimeout) && NumberUtils.isParsable(customTimeout)) {
            integer = Integer.parseInt(customTimeout);
            log.debug(String.format("Set custom VPos timeout to %s for %s", customTimeout, VPOS_TIMEOUT_STRING));
        } else {
            log.debug(String.format("Custom timeout for VPos client not found for %s. Use default value",
                    VPOS_TIMEOUT_STRING));
        }
        log.info(String.format("PGS VPos client timeout set to %s for %s", integer, VPOS_TIMEOUT_STRING));
        return integer;
    }
}

