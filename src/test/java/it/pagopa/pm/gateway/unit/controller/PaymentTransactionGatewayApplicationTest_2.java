package it.pagopa.pm.gateway.unit.controller;

import it.pagopa.pm.gateway.client.InserimentoRichiestaPagamentoPagoPaResponse;
import it.pagopa.pm.gateway.client.payment.gateway.client.BancomatPayClientV2;
import it.pagopa.pm.gateway.client.util.ClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ClientConfig.class, loader = AnnotationConfigContextLoader.class)
public class PaymentTransactionGatewayApplicationTest_2 {

    @Autowired
    BancomatPayClientV2 client;

    @Test
    public void testClient() throws Exception {

        if (client==null){
            System.out.println("CLIENT IS NULL");
        }

        InserimentoRichiestaPagamentoPagoPaResponse response =
                client.getInserimentoRichiestaPagamentoPagoPaResponse();

    }




}
