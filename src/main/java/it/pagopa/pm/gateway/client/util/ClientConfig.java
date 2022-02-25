package it.pagopa.pm.gateway.client.util;

import it.pagopa.pm.gateway.client.bpay.*;
import it.pagopa.pm.gateway.client.restapicd.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.client.*;
import org.springframework.ws.client.core.WebServiceTemplate;

import static it.pagopa.pm.gateway.client.util.Constants.BANCOMAT_PAY_CLIENT_URL;

@Slf4j
@Configuration
public class ClientConfig {

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
        webServiceTemplate.setDefaultUri(BANCOMAT_PAY_CLIENT_URL);
        log.info("bancomatPayWebServiceTemplate - bancomatPayClientUrl " + BANCOMAT_PAY_CLIENT_URL);
        return webServiceTemplate;
    }

    @Bean
    public RestapiCdClient restapiCdClient(){
        return new RestapiCdClient();
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
