package it.pagopa.pm.gateway.client;

import it.pagopa.pm.gateway.beans.*;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.config.ClientConfig;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayPaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.*;

import java.lang.Exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = ClientConfig.class, loader = AnnotationConfigContextLoader.class)
class BPayClientTests {

    @InjectMocks
    BancomatPayClient client;

    @Spy
    ObjectFactory objectFactory;

    @Mock
    WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

    @Test
    void testBpayClient() {
        InserimentoRichiestaPagamentoPagoPaResponse response = new InserimentoRichiestaPagamentoPagoPaResponse();
        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> jaxbResponse = objectFactory.createInserimentoRichiestaPagamentoPagoPaResponse(response);
        BPayPaymentRequest request = ValidBeans.bPayPaymentRequest();
        when(webServiceTemplate.marshalSendAndReceive(Mockito.any(JAXBElement.class))).thenReturn(jaxbResponse);
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