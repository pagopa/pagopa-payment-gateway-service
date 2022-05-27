package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.postepay.PostePayClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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

    @Value("${bancomatPay.client.url}")
    private String BPAY_CLIENT_URL;

    @Value("${bancomatPay.client.timeout.ms:5000}")
    private int BPAY_CLIENT_TIMEOUT_MS;

    @Value("${postePay.client.max.total:100}")
    private int MAX_TOTAL_POSTEPAY;

    @Value("${postePay.client.max.per.route:100}")
    private int MAX_PER_ROUTE_POSTEPAY;

    @Value("${postePay.client.timeout.ms:5000}")
    private int REQ_TIMEOUT_PROP_POSTEPAY;


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
    public PostePayClient postePayClient() {
        return new PostePayClient();
    }

    @Bean
    public PaymentManagerControllerApi paymentManagerControllerApi() {
            return new PaymentManagerControllerApi();
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
    public RestTemplate microsoftAzureRestTemplatePostePay(){
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory =
                new HttpComponentsClientHttpRequestFactory();
        httpComponentsClientHttpRequestFactory.setHttpClient(
                HttpClientBuilder.create()
                        .setConnectionManager(createConnectionManager(
                                MAX_TOTAL_POSTEPAY,
                                MAX_PER_ROUTE_POSTEPAY))
                        .setDefaultRequestConfig(createRequestConfig(
                                REQ_TIMEOUT_PROP_POSTEPAY))
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

}
