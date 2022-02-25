package it.pagopa.pm.gateway.unit.controller;

import it.pagopa.pm.gateway.client.EsitoVO;
import it.pagopa.pm.gateway.client.InserimentoRichiestaPagamentoPagoPaResponse;
import it.pagopa.pm.gateway.client.ResponseInserimentoRichiestaPagamentoPagoPaVO;
import it.pagopa.pm.gateway.client.payment.gateway.client.BancomatPayClientV2;
import it.pagopa.pm.gateway.client.util.ClientConfig;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import it.pagopa.pm.gateway.exception.BancomatPayClientException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.ws.client.core.WebServiceTemplate;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = ClientConfig.class, loader = AnnotationConfigContextLoader.class)
public class PaymentTransactionGatewayApplicationTest_2 {

    @InjectMocks
    BancomatPayClientV2 client;

    @Mock
    ClientConfig clientConfig;

    @Spy
    WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

    @Test
    public void testBpayClient() throws Exception {

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("it.pagopa.pm.gateway.client");

        webServiceTemplate.setMarshaller(marshaller);
        webServiceTemplate.setUnmarshaller(marshaller);
        //webServiceTemplate.setDefaultUri("http://localhost:7954/bpa");
        webServiceTemplate.setDefaultUri("https://api.dev.platform.pagopa.it/mock-psp/api/bpay");

        Mockito.when(clientConfig.bancomatPayWebServiceTemplate()).thenReturn(webServiceTemplate);

        BancomatPayPaymentRequest request = getBancomatPayPaymentRequest();

        InserimentoRichiestaPagamentoPagoPaResponse response = client.getInserimentoRichiestaPagamentoPagoPaResponse(request);

        InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponse = new InserimentoRichiestaPagamentoPagoPaResponse();
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseInserimentoRichiestaPagamentoPagoPaVO = new ResponseInserimentoRichiestaPagamentoPagoPaVO();
        responseInserimentoRichiestaPagamentoPagoPaVO.setCorrelationId(response.getReturn().getCorrelationId());
        EsitoVO esitoVO = new EsitoVO();
        esitoVO.setAvvertenza(false);
        esitoVO.setCodice("0");
        esitoVO.setEsito(true);
        esitoVO.setMessaggio("Esito positivo");
        responseInserimentoRichiestaPagamentoPagoPaVO.setEsito(esitoVO);

        inserimentoRichiestaPagamentoPagoPaResponse.setReturn(responseInserimentoRichiestaPagamentoPagoPaVO);

        Assert.assertEquals(response.getReturn().getEsito().getCodice(),
                inserimentoRichiestaPagamentoPagoPaResponse.getReturn().getEsito().getCodice());

    }

    private long generateRandomIdPagoPa(){
        long leftLimit = 1L;
        long rightLimit = 300000L;
        return leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
    }


    @Test
    public void shouldThrowBancomatPayClientException () throws BancomatPayClientException {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("it.pagopa.pm.gateway.client");

        webServiceTemplate.setMarshaller(marshaller);
        webServiceTemplate.setUnmarshaller(marshaller);
        webServiceTemplate.setDefaultUri("http://wrongUrl/bpay");

        Mockito.when(clientConfig.bancomatPayWebServiceTemplate()).thenReturn(webServiceTemplate);

        BancomatPayPaymentRequest request = getBancomatPayPaymentRequest();

        Assertions.assertThrows(BancomatPayClientException.class, () ->  client.getInserimentoRichiestaPagamentoPagoPaResponse(request));

    }

    private BancomatPayPaymentRequest getBancomatPayPaymentRequest() {
        BancomatPayPaymentRequest request = new BancomatPayPaymentRequest();
        request.setIdPsp("Id_psp");
        long idPagoPa = generateRandomIdPagoPa();
        request.setIdPagoPa(idPagoPa);
        request.setAmount(100d);
        request.setSubject("causale");
        request.setCryptedTelephoneNumber("pqimx8en49fbf");
        request.setLanguage("IT");
        return request;
    }


}