package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.vpos.CcHttpException;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.AUTHORIZED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = CcPaymentInfoService.class)
public class CcPaymentInfoServiceTest {
    
    @Mock
    private PaymentRequestRepository paymentRequestRepository;

    @Mock
    private ClientsConfig clientsConfig;

    @Spy
    @InjectMocks
    private CcPaymentInfoService ccPaymentInfoService = new CcPaymentInfoService("http://localhost:8080", paymentRequestRepository, clientsConfig);

    @Test
    public void getPaymentInfoAuthorizedSuccessTest() {
        PaymentRequestEntity paymentInfo = new PaymentRequestEntity();
        paymentInfo.setStatus(AUTHORIZED.name());
        paymentInfo.setResponseType("type");
        paymentInfo.setGuid("guid");
        paymentInfo.setAuthorizationUrl("url");
        paymentInfo.setClientId("ClientId");
        paymentInfo.setIdTransaction("1234");

        ClientConfig clientConfigToReturn = ValidBeans.createClientsConfig();

        when(paymentRequestRepository.findByGuidAndRequestEndpoint(any(), any()))
                .thenReturn(Optional.of(paymentInfo));
        when(clientsConfig.getByKey(any())).thenReturn(clientConfigToReturn);

        CcPaymentInfoResponse response = ccPaymentInfoService.getPaymentInfo("123");
        assertNotNull(response.getPaymentRequestStatusEnum());
        assertNotNull(response.getRequestId());
    }

    @Test
    public void getPaymentInfoNoAuthorizedSuccessTest() {
        PaymentRequestEntity paymentInfo = new PaymentRequestEntity();
        paymentInfo.setStatus(CREATED.name());
        paymentInfo.setResponseType("type");
        paymentInfo.setGuid("guid");
        paymentInfo.setAuthorizationUrl("url");
        paymentInfo.setResponseType("METHOD");
        paymentInfo.setIdTransaction("12345");

        when(paymentRequestRepository.findByGuidAndRequestEndpoint(any(), any()))
                .thenReturn(Optional.of(paymentInfo));

        CcPaymentInfoResponse response = ccPaymentInfoService.getPaymentInfo("123");
        assertNotNull(response.getPaymentRequestStatusEnum());
        assertNotNull(response.getRequestId());
        assertNotNull(response.getThreeDS2ResponseTypeEnum());
        assertNotNull(response.getVposUrl());
    }

    @Test
    public void getPaymentInfoChallengeSuccessTest() {
        String authUrl = "https://local?TK=tkFromVpos&creq=realcreq";
        PaymentRequestEntity paymentInfo = new PaymentRequestEntity();
        paymentInfo.setStatus(CREATED.name());
        paymentInfo.setGuid("guid");
        paymentInfo.setAuthorizationUrl(authUrl);
        paymentInfo.setResponseType("CHALLENGE");
        paymentInfo.setIdTransaction("12345");

        when(paymentRequestRepository.findByGuidAndRequestEndpoint(any(), any()))
                .thenReturn(Optional.of(paymentInfo));

        CcPaymentInfoResponse response = ccPaymentInfoService.getPaymentInfo("123");
        assertNotNull(response.getPaymentRequestStatusEnum());
        assertNotNull(response.getRequestId());
        assertNotNull(response.getThreeDS2ResponseTypeEnum());
        assertNotNull(response.getVposUrl());
        assertEquals("https://local?TK=tkFromVpos", response.getVposUrl());
        assertEquals("realcreq", response.getCreq());
    }

    @Test
    public void getPaymentInfo404Test() {
        when(paymentRequestRepository.findByGuidAndRequestEndpoint(any(), any())).thenReturn(Optional.empty());

        CcHttpException exception = assertThrows(CcHttpException.class,
                () -> ccPaymentInfoService.getPaymentInfo("123"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
