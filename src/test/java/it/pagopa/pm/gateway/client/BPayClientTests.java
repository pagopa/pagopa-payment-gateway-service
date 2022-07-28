package it.pagopa.pm.gateway.client;

import it.pagopa.pm.gateway.beans.*;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.config.ClientConfig;
import it.pagopa.pm.gateway.controller.PostePayPaymentTransactionsController;
import it.pagopa.pm.gateway.dto.BPayPaymentRequest;
import it.pagopa.pm.gateway.exception.RestApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.test.util.ReflectionTestUtils;


import javax.xml.bind.*;

import java.lang.Exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.BDDMockito.given;


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

    @MockBean
    ClientConfig clientConfig;

    @MockBean
    Environment environment;

    @Test
    void testBpayClient() throws Exception {
        ReflectionTestUtils.setField(client, "BANCOMAT_CLIENT_CONFIG",
                "groupCode|instituteCode|tag|token|http://bancomatPay:7954/bpay|5000");
        ReflectionTestUtils.setField(clientConfig, "BANCOMAT_CLIENT_CONFIG",
                "groupCode|instituteCode|tag|token|http://bancomatPay:7954/bpay|5000");

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