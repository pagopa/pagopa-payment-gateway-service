package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
import org.apache.http.impl.client.HttpClientBuilder;

import java.time.Duration;

@Slf4j
@Configuration
@EnableAsync
public class ClientConfig {

    @Value("${bancomatPay.client.url}")
    public String BPAY_CLIENT_URL;

    @Value("${bancomatPay.client.timeout.ms:5000}")
    public String BPAY_CLIENT_TIMEOUT_MS;

    @Value("${postePay.client.max.total:100}")
    public String MAX_TOTAL_POSTEPAY;

    public int DEFAULT_MAX_TOTAL_POSTEPAY = 100;

    @Value("${postePay.client.max.per.route:100}")
    public String MAX_PER_ROUTE_POSTEPAY;

    public int DEFAULT_MAX_PER_ROUTE_POSTEPAY = 100;

    @Value("${postePay.client.timeout.ms:5000}")
    public String REQ_TIMEOUT_PROP_POSTEPAY;

    public int POSTEPAY_TIMEOUT_DEFAULT = 5000;


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
    public WebServiceTemplate bancomatPayWebServiceTemplate() {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller());
        webServiceTemplate.setDefaultUri(BPAY_CLIENT_URL);
        for (WebServiceMessageSender sender : webServiceTemplate.getMessageSenders()) {
            Duration durationTimeout = Duration.ofMillis(Integer.parseInt(BPAY_CLIENT_TIMEOUT_MS));
            if (sender instanceof HttpUrlConnectionMessageSender) {
                ((HttpUrlConnectionMessageSender) sender).setConnectionTimeout(durationTimeout);
                ((HttpUrlConnectionMessageSender) sender).setReadTimeout(durationTimeout);
            }
        }
        log.info("bancomatPayWebServiceTemplate - bancomatPayClientUrl " + BPAY_CLIENT_URL);
        return webServiceTemplate;
    }

    @Bean
    public RestTemplate postePayRestTemplate(){
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory =
                new HttpComponentsClientHttpRequestFactory();
        httpComponentsClientHttpRequestFactory.setHttpClient(
                HttpClientBuilder.create()
                .setProxy(createProxy(this.getClass().getName()))
                .setConnectionManager(createConnectionManager(
                                MAX_TOTAL_POSTEPAY,
                                DEFAULT_MAX_TOTAL_POSTEPAY,
                                MAX_PER_ROUTE_POSTEPAY,
                                DEFAULT_MAX_PER_ROUTE_POSTEPAY))
                .setDefaultRequestConfig(createRequestConfig(
                                REQ_TIMEOUT_PROP_POSTEPAY,
                                POSTEPAY_TIMEOUT_DEFAULT))
                        .build());
        return new RestTemplate(httpComponentsClientHttpRequestFactory);
 }

    @Bean
    public RestTemplate microsoftAzureRestTemplate(){
        return new RestTemplate();
    }


    private HttpClientConnectionManager createConnectionManager(String maxTotalPro, int maxTotalDefault, String maxPerRoutePro, int maxPerRoutedefault) {
        PoolingHttpClientConnectionManager result = new PoolingHttpClientConnectionManager();
        result.setMaxTotal(getValueIfParsable(maxTotalPro, maxTotalDefault));
        result.setDefaultMaxPerRoute(getValueIfParsable(maxPerRoutePro, maxPerRoutedefault));
        return result;
    }


    private int getValueIfParsable(String value, int defaulValue) {
        if (StringUtils.isNotBlank(value) && NumberUtils.isParsable(value)) {
            return Integer.parseInt(value);
        }
        return defaulValue;
    }

    private RequestConfig createRequestConfig(String customRequestTimeoutProp, int defaultTimeout) {
        int reqTimeout = getValueIfParsable(customRequestTimeoutProp, defaultTimeout);

        return RequestConfig.custom()
                .setConnectionRequestTimeout(reqTimeout)
                .setConnectTimeout(reqTimeout)
                .setSocketTimeout(reqTimeout)
                .build();
    }

    private HttpHost createProxy(String className) {
        String proxyHost = System.getProperty("https.proxyHost");
        String proxyPort = System.getProperty("https.proxyPort");
        HttpHost proxy = null;
        if (StringUtils.isNoneBlank(proxyHost, proxyPort) && NumberUtils.isParsable(proxyPort)) {
            log.info(String.format("%s uses proxy: %s:%s", className, proxyHost, proxyPort));
            proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
        } else {
            log.info(String.format("%s client doesn't use proxy", className));
        }
        return proxy;
    }

}
