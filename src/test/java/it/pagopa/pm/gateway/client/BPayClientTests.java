package it.pagopa.pm.gateway.client;

import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.client.util.ClientConfig;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import it.pagopa.pm.gateway.exception.BancomatPayClientException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.*;
import java.lang.*;

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
    void testBpayClient() throws BancomatPayClientException {
        InserimentoRichiestaPagamentoPagoPaResponse response = new InserimentoRichiestaPagamentoPagoPaResponse();
        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> jaxbResponse = objectFactory.createInserimentoRichiestaPagamentoPagoPaResponse(response);
        BancomatPayPaymentRequest request = getBancomatPayPaymentRequest();
        when(webServiceTemplate.marshalSendAndReceive(Mockito.any(JAXBElement.class))).thenReturn(jaxbResponse);
        InserimentoRichiestaPagamentoPagoPaResponse actualResponse = client.sendPaymentRequest(request);
        assertEquals(response, actualResponse);
    }

    private BancomatPayPaymentRequest getBancomatPayPaymentRequest() {
        BancomatPayPaymentRequest request = new BancomatPayPaymentRequest();
        request.setIdPsp("Id_psp");
        long idPagoPa = 1 + (long) (Math.random() * 299999);
        request.setIdPagoPa(idPagoPa);
        request.setAmount(100d);
        request.setSubject("causale");
        request.setCryptedTelephoneNumber("pqimx8en49fbf");
        request.setLanguage("IT");
        return request;
    }

}