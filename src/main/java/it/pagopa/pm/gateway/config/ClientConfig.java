package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.bpay.*;
import it.pagopa.pm.gateway.client.restapicd.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.*;
import org.springframework.web.client.*;
import org.springframework.ws.client.core.WebServiceTemplate;

@Slf4j
@Configuration
@EnableAsync
public class ClientConfig {

    @Value("${bancomatPay.client.url}")
    public String BANCOMAT_PAY_CLIENT_URL;

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
    public RestapiCdClientImpl restapiCdClientImpl(){
        return new RestapiCdClientImpl();
    }

    @Bean
    public RestapiCdClientImpl restapiCdClient(){
        return new RestapiCdClientImpl();
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
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
