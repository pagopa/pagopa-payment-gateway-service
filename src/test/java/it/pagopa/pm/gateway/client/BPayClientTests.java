package it.pagopa.pm.gateway.client;

import it.pagopa.pm.gateway.beans.*;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.constant.Configurations;
import it.pagopa.pm.gateway.controller.PostePayPaymentTransactionsController;
import it.pagopa.pm.gateway.dto.BPayPaymentRequest;
import it.pagopa.pm.gateway.utils.Config;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.*;

import java.lang.Exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BancomatPayClient.class)
@AutoConfigureMockMvc
@EnableWebMvc
class BPayClientTests {

    @Autowired
    private BancomatPayClient client;

    @Spy
    ObjectFactory objectFactory;

    @MockBean
    private WebServiceTemplate webServiceTemplate;

    @MockBean
    private Config config;

    @Test
    void testBpayClient() {
        InserimentoRichiestaPagamentoPagoPaResponse response = new InserimentoRichiestaPagamentoPagoPaResponse();
        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> jaxbResponse = objectFactory.createInserimentoRichiestaPagamentoPagoPaResponse(response);
        BPayPaymentRequest request = ValidBeans.bPayPaymentRequest();
        when(webServiceTemplate.marshalSendAndReceive(Mockito.any(JAXBElement.class))).thenReturn(jaxbResponse);
//        given(config.getConfig(Configurations.BANCOMATPAY_CLIENT_TOKEN)).willReturn("");
        when(config.getConfig(Configurations.BANCOMATPAY_CLIENT_TOKEN)).thenReturn("");
        InserimentoRichiestaPagamentoPagoPaResponse actualResponse = client.sendPaymentRequest(request, "null-null-null-null-null");
        assertEquals(response, actualResponse);
    }

    @Test
    void shouldReturnBancomatPayClientException() {
        BPayPaymentRequest request = ValidBeans.bPayPaymentRequest();
        webServiceTemplate.setDefaultUri("http://incorrectUrl");
        Assertions.assertThrows(Exception.class,()-> client.sendPaymentRequest(request, "null-null-null-null-null"));
    }

}