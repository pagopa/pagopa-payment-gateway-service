package it.pagopa.pm.gateway.unit.controller;

import it.pagopa.pm.gateway.client.wsdl.generated.files.InserimentoRichiestaPagamentoPagoPaResponse;
import it.pagopa.pm.gateway.client.payment.gateway.client.BancomatPayClientV2;
import it.pagopa.pm.gateway.client.util.ClientConfig;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@Slf4j
//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest(System.class)
@SpringBootTest
@ContextConfiguration(classes = ClientConfig.class, loader = AnnotationConfigContextLoader.class)
public class PaymentTransactionGatewayApplicationTest_2 {

    @Autowired
    BancomatPayClientV2 client;


    @Test
    public void testClient() throws Exception {

        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getProperty("bancomatPay.client.url")).thenReturn("http://bancomatPay:7954/bpay");

        if (client == null) {
            System.out.println("CLIENT IS NULL");
        }

        BancomatPayPaymentRequest request = new BancomatPayPaymentRequest();
        request.setIdPsp("Id_psp");
        request.setIdPagoPa("ervaid");
        request.setAmount(100d);
        request.setSubject("causale");
        request.setCryptedTelephoneNumber("pqimx8en49fbf");
        request.setLanguage("IT");

        String bb = System.getProperty("bancomatPay.client.url");
        log.info("URL:::::::::::::::::::::::::::" + bb);

        InserimentoRichiestaPagamentoPagoPaResponse response =
                client.getInserimentoRichiestaPagamentoPagoPaResponse(request);

    }

}