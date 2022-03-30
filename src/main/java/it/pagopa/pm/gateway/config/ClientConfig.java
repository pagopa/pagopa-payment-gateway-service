package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    public static int BPAY_CLIENT_TIMEOUT_MS_DEFAULT = 5000;

    @Value("${bancomatPay.client.url}")
    public String BPAY_CLIENT_URL;

    @Value("${bancomatPay.client.timeout.ms}")
    public String BPAY_CLIENT_TIMEOUT_MS;

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
        int timeout = BPAY_CLIENT_TIMEOUT_MS_DEFAULT;
        try {
            timeout = Integer.parseInt(BPAY_CLIENT_TIMEOUT_MS);
        } catch (NumberFormatException ex) {
            log.error("Unable to parse BPAY_CLIENT_TIMEOUT_MS " +  BPAY_CLIENT_TIMEOUT_MS + " - using default timeout");
        }
        for (WebServiceMessageSender sender : webServiceTemplate.getMessageSenders()) {
            Duration durationTimeout = Duration.ofMillis(timeout);
            if (sender instanceof HttpUrlConnectionMessageSender) {
                ((HttpUrlConnectionMessageSender) sender).setConnectionTimeout(durationTimeout);
                ((HttpUrlConnectionMessageSender) sender).setReadTimeout(durationTimeout);
            }
        }
        log.info("bancomatPayWebServiceTemplate - bancomatPayClientUrl " + BPAY_CLIENT_URL);
        return webServiceTemplate;
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
