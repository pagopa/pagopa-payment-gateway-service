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
import org.openapitools.client.api.UserApi;
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
import java.util.Objects;

@Slf4j
@Configuration
public class ClientConfig {

    private static final String HTTPS_PROXY_HOST_PROPERTY = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT_PROPERTY = "https.proxyPort";
    private static final int XPAY_DEFAULT_TIMEOUT = 10000;
    private static final int XPAY_DEFAULT_MAX_TOTAL = 100;
    private static final int XPAY_DEFAULT_MAX_PER_ROUTE = 50;

    @Value("${bancomatPay.client.url}")
    private String bpayClientUrl;

    @Value("${bancomatPay.client.timeout.ms:5000}")
    private int bpayClientTimeoutMs;

    @Value("${azureAuth.client.maxTotal:100}")
    private int azureClientMaxTotal;

    @Value("${azureAuth.client.maxPerRoute:100}")
    private int azureClientMaxPerRoute;

    @Value("${azureAuth.client.timeout.ms:5000}")
    private int azureClientTimeout;

    @Value("${postepay.client.url}")
    private String postepayClientUrl;

    @Value("${postepay.client.timeout.ms:5000}")
    private int postepayClientTimeout;

    @Value("${pgs.xpay.client.maxPerRoute}")
    private String xpayMaxPerRoute;

    @Value("${pgs.xpay.client.maxTotal}")
    private String xpayMaxConnection;

    @Value("${pgs.xpay.client.timeOut}")
    private String xpayClientTimeout;

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
        ApiClient apiClient = createApiClient();
        log.info("END - postePayControllerApi()- POSTEPAY_CLIENT_URL: " + postepayClientUrl);
        return new PaymentManagerControllerApi(apiClient);
    }

    @Bean
    public UserApi postePayUserApi() {
        log.info("START postePayUserApi()");
        ApiClient apiClient = createApiClient();
        log.info("END - postePayUserApi()- POSTEPAY_CLIENT_URL: " + postepayClientUrl);
        return new UserApi(apiClient);
    }

    private ApiClient createApiClient() {
        return addProxyToApiClient(new ApiClient()
                .setBasePath(postepayClientUrl)
                .setConnectTimeout(postepayClientTimeout));
    }

    @Bean
    public WebServiceTemplate bancomatPayWebServiceTemplate() {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller());
        webServiceTemplate.setDefaultUri(bpayClientUrl);
        for (WebServiceMessageSender sender : webServiceTemplate.getMessageSenders()) {
            Duration durationTimeout = Duration.ofMillis(bpayClientTimeoutMs);
            if (sender instanceof HttpUrlConnectionMessageSender) {
                ((HttpUrlConnectionMessageSender) sender).setConnectionTimeout(durationTimeout);
                ((HttpUrlConnectionMessageSender) sender).setReadTimeout(durationTimeout);
            }
        }
        log.info("bancomatPayWebServiceTemplate - bancomatPayClientUrl " + bpayClientUrl);
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
                                azureClientMaxTotal,
                                azureClientMaxPerRoute))
                        .setDefaultRequestConfig(createRequestConfig(
                                azureClientTimeout))
                        .build());
        return new RestTemplate(httpComponentsClientHttpRequestFactory);
    }

    @Bean
    public RestTemplate xpayRestTemplate() {
        int maxTotal = getValueIfParsable(xpayMaxConnection, XPAY_DEFAULT_MAX_TOTAL);
        int maxPerRoute = getValueIfParsable(xpayMaxPerRoute, XPAY_DEFAULT_MAX_PER_ROUTE);
        int timeout = getValueIfParsable(xpayClientTimeout, XPAY_DEFAULT_TIMEOUT);

        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory =
                new HttpComponentsClientHttpRequestFactory();
        httpComponentsClientHttpRequestFactory.setHttpClient(
                HttpClientBuilder.create()
                    .setProxy(createProxy(this.getClass().getName()))
                    .setConnectionManager(createConnectionManager(
                            maxTotal,
                            maxPerRoute))
                    .setDefaultRequestConfig(createRequestConfig(timeout))
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

    private static int getValueIfParsable(String value, int defaulValue) {
        if (StringUtils.isNotBlank(value) && NumberUtils.isParsable(value)) {
            return Integer.parseInt(value);
        }
        return defaulValue;
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
