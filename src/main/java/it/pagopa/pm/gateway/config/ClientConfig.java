package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.azure.AzureLoginClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.HttpHost;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.PaymentManagerControllerApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ClientConfigs.*;

@Slf4j
@Configuration
@EnableAsync
public class ClientConfig {

    private static final String HTTPS_PROXY_HOST_PROPERTY = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT_PROPERTY = "https.proxyPort";

    @Value("${postepay.client.url}")
    private String POSTEPAY_CLIENT_URL;

    @Value("${postepay.client.timeout.ms:5000}")
    private int POSTEPAY_CLIENT_TIMEOUT;

    @Value("${azureAuth.client.config")
    private String AZUREAUTH_CLIENT_CONFIG;

    @Value("${bancomatPay.client.config}")
    private String BANCOMAT_CLIENT_CONFIG;

    private final Map<String, String> azureAuthClientConfigValues;
    private final Map<String, String> bancomatClientConfigValues;

    {
        azureAuthClientConfigValues = getAzureAuthClientConfigValues();
        bancomatClientConfigValues = getBancomatClientConfigValues();
    }

    private Map<String, String> getAzureAuthClientConfigValues() {
        List<String> listConfig = Arrays.asList(AZUREAUTH_CLIENT_CONFIG.split(PIPE_SPLIT_CHAR));
        Map<String, String> configsMap = new HashMap<>();
        configsMap.put(MAX_TOTAL, listConfig.get(0));
        configsMap.put(MAX_PER_ROUTE, listConfig.get(1));
        configsMap.put(TIMEOUT_MS, listConfig.get(2));

        return configsMap;
    }

    private Map<String, String> getBancomatClientConfigValues() {
        List<String> listConfig = Arrays.asList(BANCOMAT_CLIENT_CONFIG.split(PIPE_SPLIT_CHAR));
        Map<String, String> configsMap = new HashMap<>();
        configsMap.put(URL, listConfig.get(4));
        configsMap.put(TIMEOUT_MS, listConfig.get(5));

        return configsMap;
    }

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("it.pagopa.pm.gateway.client.bpay.generated");
        return marshaller;
    }

    @Bean
    public BancomatPayClient bancomatPayClient(Jaxb2Marshaller marshaller) {
        return new BancomatPayClient();
    }

    @Bean
    public AzureLoginClient azureLoginClient() {
        return new AzureLoginClient();
    }

    @Bean
    public PaymentManagerControllerApi postePayControllerApi() {
  log.info("START postePayControllerApi()");
        ApiClient apiClient = addProxyToApiClient(new ApiClient()
                .setBasePath(POSTEPAY_CLIENT_URL)
                .setConnectTimeout(POSTEPAY_CLIENT_TIMEOUT));
       log.info("END - postePayControllerApi()- POSTEPAY_CLIENT_URL: " + POSTEPAY_CLIENT_URL);
        return new PaymentManagerControllerApi(apiClient);
    }

    @Bean
    public WebServiceTemplate bancomatPayWebServiceTemplate() {
       String url = bancomatClientConfigValues.get(URL);

        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller());
        webServiceTemplate.setDefaultUri(url);
        for (WebServiceMessageSender sender : webServiceTemplate.getMessageSenders()) {
            Duration durationTimeout = Duration.ofMillis(Long.parseLong(bancomatClientConfigValues.get(TIMEOUT_MS)));
            if (sender instanceof HttpUrlConnectionMessageSender) {
                ((HttpUrlConnectionMessageSender) sender).setConnectionTimeout(durationTimeout);
                ((HttpUrlConnectionMessageSender) sender).setReadTimeout(durationTimeout);
            }
        }
        log.info("bancomatPayWebServiceTemplate - bancomatPayClientUrl " + url);
        return webServiceTemplate;
    }

    @Bean
    public RestTemplate microsoftAzureRestTemplate() {

        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory =
                new HttpComponentsClientHttpRequestFactory();
        httpComponentsClientHttpRequestFactory.setHttpClient(
                HttpClientBuilder.create()
                        .setProxy(createProxy(this.getClass().getName()))
                        .setConnectionManager(createConnectionManager(
                                Integer.parseInt(azureAuthClientConfigValues.get(MAX_TOTAL)),
                                Integer.parseInt(azureAuthClientConfigValues.get(MAX_PER_ROUTE))))
                        .setDefaultRequestConfig(createRequestConfig(
                                Integer.parseInt(azureAuthClientConfigValues.get(TIMEOUT_MS))))
                        .build());
        return new RestTemplate(httpComponentsClientHttpRequestFactory);
    }

    private HttpClientConnectionManager createConnectionManager(int maxTotalPro, int maxPerRoutePro) {
        PoolingHttpClientConnectionManager result = new PoolingHttpClientConnectionManager();
        result.setMaxTotal(maxTotalPro);
        result.setDefaultMaxPerRoute(maxPerRoutePro);
        return result;
    }

    private RequestConfig createRequestConfig(int reqTimeout) {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(reqTimeout)
                .setConnectTimeout(reqTimeout)
                .setSocketTimeout(reqTimeout)
                .build();
    }

    public static HttpHost createProxy(String className) {
        String proxyHost = System.getProperty(HTTPS_PROXY_HOST_PROPERTY);
        String proxyPort = System.getProperty(HTTPS_PROXY_PORT_PROPERTY);
        HttpHost proxy = null;
        if (StringUtils.isNoneBlank(proxyHost, proxyPort) && NumberUtils.isParsable(proxyPort)) {
            log.info(String.format("%s uses proxy: %s:%s", className, proxyHost, proxyPort));
            proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
        } else {
            log.info(String.format("%s client does not use proxy", className));
        }
        return proxy;
    }

    public static ApiClient addProxyToApiClient(ApiClient apiClient) {
        HttpHost host = createProxy(ClientConfig.class.getName());
        if (!Objects.isNull(host)) {
            InetSocketAddress proxyAddress = new InetSocketAddress(host.getHostName(), host.getPort());
            Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
            OkHttpClient httpClient = apiClient.getHttpClient().newBuilder().proxy(proxy).build();
            return apiClient.setHttpClient(httpClient);
        }
        return apiClient;
    }
}
