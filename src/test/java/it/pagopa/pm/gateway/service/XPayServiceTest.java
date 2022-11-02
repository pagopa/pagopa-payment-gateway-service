package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.dto.xpay.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CREATED;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = XpayService.class)
public class XPayServiceTest {

    @Spy
    @InjectMocks
    private XpayService service = new XpayService();
    @Mock
    private RestTemplate xpayRestTemplate;

    private static final String APP_ORIGIN = "APP";

    @Before
    public void setUpProperties() {
        ReflectionTestUtils.setField(service, "xpayAuthUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "azureApiKey", "apiKey");
        ReflectionTestUtils.setField(service, "xpayPaymentUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "orderStatusUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "revertUrl", "http://localhost:8080");
    }

    @Test
    public void callAutenticazione3DSTest() {
        try {

            XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
            AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);

            AuthPaymentXPayResponse responseTest = ValidBeans.createXPayAuthResponse(authPaymentXPayRequest);
            when(xpayRestTemplate.postForObject(anyString(), any(), any())).thenReturn(responseTest);
            AuthPaymentXPayResponse responseService = service.callAutenticazione3DS(authPaymentXPayRequest);
            assertEquals(responseService, responseTest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void callPaga3DSTest() {
        try {
            XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
            PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true, CREATED, false);
            PaymentXPayRequest request = ValidBeans.createXPayPaymentRequest(entity);

            PaymentXPayResponse responseTest = ValidBeans.createPaymentXPayResponse(true);
            when(xpayRestTemplate.postForObject(anyString(), any(), any())).thenReturn(responseTest);
            PaymentXPayResponse responseService = service.callPaga3DS(request);
            assertEquals(responseService, responseTest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void callSituazioneOrdineTest() {
        try {
            XPayOrderStatusRequest request = ValidBeans.createXPayOrderStatusRequest();

            XPayOrderStatusResponse responseTest = ValidBeans.createXPayOrderStatusResponse(true);
            when(xpayRestTemplate.postForObject(anyString(), any(), any())).thenReturn(responseTest);
            XPayOrderStatusResponse responseService = service.callSituazioneOrdine(request);
            assertEquals(responseService, responseTest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void callStornaTest() {
        try {
            XPayRevertRequest request = ValidBeans.createXPayRevertRequest();

            XPayRevertResponse responseTest = ValidBeans.createXPayRevertResponse(true);
            when(xpayRestTemplate.postForObject(anyString(), any(), any())).thenReturn(responseTest);
            XPayRevertResponse responseService = service.callStorna(request);
            assertEquals(responseService, responseTest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

