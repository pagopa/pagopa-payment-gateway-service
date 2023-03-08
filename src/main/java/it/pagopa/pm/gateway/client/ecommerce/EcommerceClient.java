package it.pagopa.pm.gateway.client.ecommerce;

import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.transaction.TransactionInfo;
import it.pagopa.pm.gateway.dto.transaction.UpdateAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class EcommerceClient {

    private static final String CONTEXT = "ECOMMERCE_CLIENT_";
    private static final String MAX_TOTAL_CONNECTIONS = System.getProperty(CONTEXT + "MAX_TOTAL");
    private static final String MAX_CONNECTIONS_PER_ROUTE = System.getProperty(CONTEXT + "MAX_PER_ROUTE");
    private static final String REQUEST_TIMEOUT_MSEC = System.getProperty(CONTEXT + "REQUEST_TIMEOUT");
    private static final String CONNECTION_TIMEOUT_MSEC = System.getProperty(CONTEXT + "CONNECT_TIMEOUT");
    private static final String SOCKET_TIMEOUT_MSEC = System.getProperty(CONTEXT + "SOCKET_TIMEOUT");
    private static final String HEADER_API_KEY = "ocp-apim-subscription-key";

    private RestTemplate eCommerceRestTemplate;

    @PostConstruct
    public void initEcommerceClient() {
        log.info("Building eCommerce client");
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(getIntFromValue(MAX_TOTAL_CONNECTIONS, 1024));
        connectionManager.setDefaultMaxPerRoute(getIntFromValue(MAX_CONNECTIONS_PER_ROUTE, 1024));

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(getIntFromValue(REQUEST_TIMEOUT_MSEC, 10000))
                .setConnectTimeout(getIntFromValue(CONNECTION_TIMEOUT_MSEC, 10000))
                .setSocketTimeout(getIntFromValue(SOCKET_TIMEOUT_MSEC, 10000))
                .build();

        HttpComponentsClientHttpRequestFactory client = new HttpComponentsClientHttpRequestFactory();
        client.setHttpClient(HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build());

        eCommerceRestTemplate = new RestTemplate(client);

        log.info(String.format("eCommerce client built with following parameters:" +
                        "\nmax total connections: %s" +
                        "\nmax connections per route: %s" +
                        "\nrequest timeout: %s msec" +
                        "\nconnection timeout: %s msec" +
                        "\nsocket timeout: %s msec",
                connectionManager.getMaxTotal(), connectionManager.getDefaultMaxPerRoute(),
                requestConfig.getConnectionRequestTimeout(), requestConfig.getConnectTimeout(),
                requestConfig.getSocketTimeout()));
    }

    private int getIntFromValue(String value, int defaultValue) {
        return NumberUtils.isParsable(value) ? Integer.parseInt(value) : defaultValue;
    }

    public TransactionInfo callPatchTransaction(UpdateAuthRequest request, String transactionId, ClientConfig clientConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(clientConfig.getAuthorizationUpdateApiKey())) {
            headers.add(HEADER_API_KEY, clientConfig.getAuthorizationUpdateApiKey());
        }

        HttpEntity<UpdateAuthRequest> entity = new HttpEntity<>(request, headers);
        String transactionPatchUrl = String.format(clientConfig.getAuthorizationUpdateUrl(), transactionId);

        log.info("Calling PATCH for transaction " + transactionId + " with authorizationCode " + request.getAuthorizationCode());
        return eCommerceRestTemplate.patchForObject(transactionPatchUrl, entity, TransactionInfo.class);
    }
}
