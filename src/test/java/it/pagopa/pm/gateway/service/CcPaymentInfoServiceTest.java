package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.dto.vpos.CcHttpException;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = CcPaymentInfoService.class)
public class CcPaymentInfoServiceTest {
    @InjectMocks
    private CcPaymentInfoService ccPaymentInfoService;

    @Mock
    private PaymentRequestRepository paymentRequestRepository;

    @Test
    public void getPaymentInfoSuccessTest() {
        PaymentRequestEntity paymentInfo = new PaymentRequestEntity();
        paymentInfo.setStatus("status");
        paymentInfo.setResponseType("type");
        paymentInfo.setGuid("guid");
        paymentInfo.setAuthorizationUrl("url");

        when(paymentRequestRepository.findByGuidAndRequestEndpoint(any(), any()))
                .thenReturn(Optional.of(paymentInfo));

        CcPaymentInfoResponse response = ccPaymentInfoService.getPaymentoInfo("123");
        assertNotNull(response.getStatus());
        assertNotNull(response.getRequestId());
        assertNotNull(response.getResponseType());
        assertNotNull(response.getAcsUrl());
    }

    @Test
    public void getPaymentInfo404Test() {
        when(paymentRequestRepository.findByGuidAndRequestEndpoint(any(), any())).thenReturn(Optional.empty());

        CcHttpException exception = assertThrows(CcHttpException.class,
                () -> ccPaymentInfoService.getPaymentoInfo("123"));

        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
    }
}
