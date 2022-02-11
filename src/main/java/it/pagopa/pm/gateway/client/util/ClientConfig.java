package it.pagopa.pm.gateway.client.util;

import it.pagopa.pm.gateway.client.payment.gateway.client.BancomatPayClientV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class ClientConfig {

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("it.pagopa.pm.gateway.client");
        return marshaller;
    }

    @Bean
    public BancomatPayClientV2 bancomatPayClientV2(Jaxb2Marshaller marshaller) {
        BancomatPayClientV2 client = new BancomatPayClientV2();
        client.setDefaultUri("localhost:7954/srv/pp/inserimentoRichiestaPagamentoPagoPa");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }

}
