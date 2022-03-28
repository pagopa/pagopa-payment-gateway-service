package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.bpay.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.*;
import org.springframework.web.client.*;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.*;

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
        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        int timeout = BPAY_CLIENT_TIMEOUT_MS_DEFAULT;
        try {
            timeout = Integer.parseInt(BPAY_CLIENT_TIMEOUT_MS);
        } catch (NumberFormatException ex)
        {
            log.error("Unable to parse BPAY_CLIENT_TIMEOUT_MS " +  BPAY_CLIENT_TIMEOUT_MS + " - using default timeout");
        }
        sender.setConnectionTimeout(timeout);
        sender.setReadTimeout(timeout);
        webServiceTemplate.setMessageSender(sender);
        log.info("bancomatPayWebServiceTemplate - bancomatPayClientUrl " + BPAY_CLIENT_URL);
        return webServiceTemplate;
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
