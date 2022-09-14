package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.azure.AzureLoginClient;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.constant.Configurations;
import it.pagopa.pm.gateway.utils.Config;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.PaymentManagerControllerApi;
import org.openapitools.client.api.UserApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@Configuration
@EnableAsync
public class ClientConfig {

    private static String PROXY_HOST;
    private static String PROXY_PORT;

    @PostConstruct
    protected void init() {
        PROXY_HOST = config.getConfig(Configurations.HTTPS_PROXYHOST);
        PROXY_PORT = config.getConfig(Configurations.HTTPS_PROXYPORT);
    }

    @Autowired
    private Config config;

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
        log.info("END - postePayControllerApi()- POSTEPAY_CLIENT_URL: " + config.getConfig(Configurations.POSTEPAY_CLIENT_URL));
        return new PaymentManagerControllerApi(apiClient);
    }

    @Bean
    public UserApi postePayUserApi() {
        log.info("START postePayUserApi()");
        ApiClient apiClient = createApiClient();
        log.info("END - postePayUserApi()- POSTEPAY_CLIENT_URL: " + config.getConfig(Configurations.POSTEPAY_CLIENT_URL));
        return new UserApi(apiClient);
    }

    private ApiClient createApiClient() {
        return addProxyToApiClient(new ApiClient()
                .setBasePath(config.getConfig(Configurations.POSTEPAY_CLIENT_URL))
                .setConnectTimeout(Integer.parseInt(config.getConfig(Configurations.POSTEPAY_CLIENT_TIMEOUT_MS))));
    }

    @Bean
    public WebServiceTemplate bancomatPayWebServiceTemplate() {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller());
        webServiceTemplate.setDefaultUri(config.getConfig(Configurations.BANCOMATPAY_CLIENT_URL));
        for (WebServiceMessageSender sender : webServiceTemplate.getMessageSenders()) {
            Duration durationTimeout = Duration.ofMillis(Long.parseLong(config.getConfig(Configurations.BANCOMATPAY_CLIENT_TIMEOUT_MS)));
            if (sender instanceof HttpUrlConnectionMessageSender) {
                ((HttpUrlConnectionMessageSender) sender).setConnectionTimeout(durationTimeout);
                ((HttpUrlConnectionMessageSender) sender).setReadTimeout(durationTimeout);
            }
        }
        log.info("bancomatPayWebServiceTemplate - bancomatPayClientUrl " + config.getConfig(Configurations.BANCOMATPAY_CLIENT_URL));
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
                                Integer.parseInt(config.getConfig(Configurations.AZUREAUTH_CLIENT_MAXTOTAL)),
                                Integer.parseInt(config.getConfig(Configurations.AZUREAUTH_CLIENT_MAXPERROUTE))))
                        .setDefaultRequestConfig(createRequestConfig(
                                Integer.parseInt(config.getConfig(Configurations.AZUREAUTH_CLIENT_TIMEOUT_MS))))
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
        HttpHost proxy = null;
        if (StringUtils.isNoneBlank(PROXY_HOST, PROXY_PORT) && NumberUtils.isParsable(PROXY_PORT)) {
            log.info(String.format("%s uses proxy: %s:%s", className, PROXY_HOST, PROXY_PORT));
            proxy = new HttpHost(PROXY_HOST, Integer.parseInt(PROXY_PORT));
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
