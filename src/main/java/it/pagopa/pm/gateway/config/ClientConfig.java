package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.azure.AzureLoginClient;
import lombok.extern.slf4j.Slf4j;
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

import java.time.Duration;

@Slf4j
@Configuration
@EnableAsync
public class ClientConfig {

    private static final String HTTPS_PROXY_HOST_PROPERTY = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT_PROPERTY = "https.proxyPort";
    @Value("${bancomatPay.client.url}")
    private String BPAY_CLIENT_URL;

    @Value("${bancomatPay.client.timeout.ms:5000}")
    private int BPAY_CLIENT_TIMEOUT_MS;

    @Value("${azureAuth.client.maxTotal:100}")
    private int AZURE_CLIENT_MAX_TOTAL;

    @Value("${azureAuth.client.maxPerRoute:100}")
    private int AZURE_CLIENT_MAX_PER_ROUTE;

    @Value("${azureAuth.client.timeout.ms:5000}")
    private int AZURE_CLIENT_TIMEOUT;

    @Value("${postepay.client.url}")
    private String POSTEPAY_CLIENT_URL;

    @Value("${postepay.client.timeout.ms:5000}")
    private int POSTEPAY_CLIENT_TIMEOUT;

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
        return new PaymentManagerControllerApi(new ApiClient().setBasePath(POSTEPAY_CLIENT_URL).setConnectTimeout(POSTEPAY_CLIENT_TIMEOUT));
    }

    @Bean
    public WebServiceTemplate bancomatPayWebServiceTemplate() {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller());
        webServiceTemplate.setDefaultUri(BPAY_CLIENT_URL);
        for (WebServiceMessageSender sender : webServiceTemplate.getMessageSenders()) {
            Duration durationTimeout = Duration.ofMillis(BPAY_CLIENT_TIMEOUT_MS);
            if (sender instanceof HttpUrlConnectionMessageSender) {
                ((HttpUrlConnectionMessageSender) sender).setConnectionTimeout(durationTimeout);
                ((HttpUrlConnectionMessageSender) sender).setReadTimeout(durationTimeout);
            }
        }
        log.info("bancomatPayWebServiceTemplate - bancomatPayClientUrl " + BPAY_CLIENT_URL);
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
                                AZURE_CLIENT_MAX_TOTAL,
                                AZURE_CLIENT_MAX_PER_ROUTE))
                        .setDefaultRequestConfig(createRequestConfig(
                                AZURE_CLIENT_TIMEOUT))
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
}
