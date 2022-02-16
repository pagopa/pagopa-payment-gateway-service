package it.pagopa.pm.gateway.client.util;

import it.pagopa.pm.gateway.client.payment.gateway.client.BancomatPayClientV2;
import it.pagopa.pm.gateway.client.restapiCD.RestapiCdClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration
public class ClientConfig {

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("it.pagopa.pm.gateway.client");
        return marshaller;
    }

    @Bean
    public BancomatPayClientV2 bancomatPayClientV2(Jaxb2Marshaller marshaller) {
        BancomatPayClientV2 client = new BancomatPayClientV2();
        return client;
    }


    @Bean
    public WebServiceTemplate webServiceTemplate() {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller());
        webServiceTemplate.setDefaultUri("http://localhost:7954/bpay");

        return webServiceTemplate;
    }

    @Bean
    public RestapiCdClient restapiCdClient(){
        RestapiCdClient restapiCdClient = new RestapiCdClient();
        return  restapiCdClient;

    }

    @Bean
    public RestTemplate restTemplate(){
        RestTemplate restTemplate = new RestTemplate();
        return  restTemplate;

    }

}
