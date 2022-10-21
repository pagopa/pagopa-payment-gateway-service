package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.constant.Headers;
import it.pagopa.pm.gateway.dto.XPayAuthRequest;
import it.pagopa.pm.gateway.dto.XPayPollingResponseError;
import it.pagopa.pm.gateway.dto.xpay.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.XpayService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.UUID;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_XPAY;
import static it.pagopa.pm.gateway.constant.ApiPaths.XPAY_AUTH;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.constant.XPayParams.XPAY_MAC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = XPayPaymentController.class)
@AutoConfigureMockMvc
@EnableWebMvc
@TestPropertySource(properties = {"xpay.response.urlredirect=http://localhost:8080/payment-gateway/"})
public class XPayPaymentControllerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @MockBean
    private PaymentRequestRepository paymentRequestRepository;
    @MockBean
    private XpayService xpayService;
    @MockBean
    private RestapiCdClientImpl restapiCdClient;

    @Autowired
    private MockMvc mvc;

    private final String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";

    @Mock
    final UUID uuid = UUID.fromString(UUID_SAMPLE);

    @Mock
    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    private static final String APP_ORIGIN = "APP";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void xPay_givenGoodRequest_shouldReturnOkResponse() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        AuthPaymentXPayRequest xPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        AuthPaymentXPayResponse xPayResponse = ValidBeans.createXPayAuthResponse(xPayRequest);

        when(xpayService.callAutenticazione3DS(any())).thenReturn(xPayResponse);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true));

        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void xPay_ReceivingErrorFromXPay_shouldReturnOkResponseAndStatusDenied() throws Exception {
        try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class)) {

            mockedUuid.when(UUID::randomUUID).thenReturn(uuid);
            when(uuid.toString()).thenReturn(UUID_SAMPLE);

            XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
            AuthPaymentXPayRequest xPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
            AuthPaymentXPayResponse xPayResponse = ValidBeans.createXPayAuthResponseError(xPayRequest);
            PaymentRequestEntity entity = ValidBeans.paymentRequestEntityXpayDenied(xPayAuthRequest, APP_ORIGIN);

            entity.setGuid(UUID_SAMPLE);

            when(paymentRequestRepository.findByGuid(any())).
                    thenReturn(entity);

            when(xpayService.callAutenticazione3DS(any())).thenReturn(xPayResponse);

            mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                            .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                            .content(mapper.writeValueAsString(xPayAuthRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(mapper.writeValueAsString(ValidBeans.xPayAuthResponse(false, null, UUID_SAMPLE, true))));
        }
    }

    @Test
    public void xPay_givenGoodRequest_shouldThrowResourceAccessException() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(xpayService.callAutenticazione3DS(any())).thenThrow(ResourceAccessException.class);

        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void xPay_givenRequestWithGrandTotalEqualToZero_shouldReturnBadRequestResponse() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(false);
        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.xPayAuthResponse(true, BAD_REQUEST_MSG, null, false))));
    }

    @Test
    public void xPay_givenRequestWithInvalidClientId_shouldReturnBadRequestClientIdResponse() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, "XXX")
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.xPayAuthResponse(true, BAD_REQUEST_MSG_CLIENT_ID, null, false))));
    }

    @Test
    public void xPay_givenRequestWithAlreadyProcessedTransaction_shouldReturnAlreadyProcessedTransactionResponse() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);

        given(paymentRequestRepository.findByIdTransaction("2")).
                willReturn(ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true));

        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, "APP")
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.xPayAuthResponse(true, TRANSACTION_ALREADY_PROCESSED_MSG, null, false))));
    }

    @Test
    public void xPay_givenResponseNull_shouldThrowException() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(xpayService.callAutenticazione3DS(any())).thenReturn(null);

        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void xPay_shouldReturnAuthPollingResponseOK() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(paymentRequestRepository.findByGuid(UUID_SAMPLE))
                .thenReturn(ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true));

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.createXpayAuthPollingResponse(true, null, false))));
    }

    @Test
    public void xPay_shouldReturnAuthPollingResponseKO() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity requestEntity = ValidBeans.paymentRequestEntityxPayWithError(xPayAuthRequest, APP_ORIGIN);
        when(paymentRequestRepository.findByGuid(UUID_SAMPLE))
                .thenReturn(requestEntity);

        XPayPollingResponseError error = new XPayPollingResponseError(Long.valueOf(requestEntity.getErrorCode()), requestEntity.getErrorMessage());

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.createXpayAuthPollingResponse(false, error, false))));
    }

    @Test
    public void xPay_givenRequestEntityWithoutHtmlAndWithoutError_shouldReturnPending() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(paymentRequestRepository.findByGuid(UUID_SAMPLE))
                .thenReturn(ValidBeans.paymentRequestEntityxPayWithoutHtml(xPayAuthRequest, APP_ORIGIN));

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.createXpayAuthPollingResponse(false, null, true))));
    }

    @Test
    public void xPay_givenInvalidRequestId_shouldReturnNotFound() throws Exception {
        when(paymentRequestRepository.findByGuid(any())).thenReturn(null);

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE))
                .andExpect(status().isNotFound());

    }

    @Test
    public void xPay_givenGoodResumeRequest_shouldReturn302Status() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(params.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);


        PaymentXPayResponse xPayResponse = ValidBeans.createPaymentXPayResponse(true);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        when(xpayService.callPaga3DS(any())).thenReturn(xPayResponse);

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume/")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isFound());
    }

    @Test
    public void xPay_givenResumeRequestWithKO_shouldReturnResponseFromXPayOKAnd302Status() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequest(false);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(params.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume/")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isFound());
    }

    @Test
    public void xPay_givenResumeRequestWithEsitoEqualToNull_shouldReturn400Status() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequestWithEsitoNull();

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void xPay_givenGoodResumeRequest_shouldReturn4042Status() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequest(true);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(null);

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isNotFound());
    }

    @Test
    public void xPay_givenGoodResumeRequest_shouldReturnResponseFromXPAyKOAnd302Status() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(params.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);


        PaymentXPayResponse xPayResponse = ValidBeans.createPaymentXPayResponse(false);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        when(xpayService.callPaga3DS(any())).thenReturn(xPayResponse);

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isFound());
    }

    @Test
    public void xPay_givenGoodResumeRequest_shouldReturn401Status() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequest(false);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(params.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);
        entity.setAuthorizationOutcome(true);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void xPay_givenGoodResumeRequest_shouldExecuteTheRetry() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(params.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        when(xpayService.callPaga3DS(any())).thenThrow(new RuntimeException());

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isFound());
    }

    @Test
    public void xPay_givenGoodResumeRequest_shouldFailClosePaymentAndReturn302Status() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(params.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);

        PaymentXPayResponse xPayResponse = ValidBeans.createPaymentXPayResponse(true);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        when(xpayService.callPaga3DS(any())).thenReturn(xPayResponse);

        when(restapiCdClient.callPatchTransactionV2(any(), any())).thenThrow(new RuntimeException());

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isFound());
    }

    @Test
    public void xPay_givenGoodResumeRequest_shouldThrowExpetionAdnReturn500() throws Exception {
        MultiValueMap<String, String> params = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(params.get(XPAY_MAC)));
        entity.setJsonRequest(null);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        mvc.perform(get(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE + "/resume")
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isInternalServerError());
    }


    @Test
    public void xPay_givenBadRequestIdForResume_shouldReturn404() throws Exception {

        when(paymentRequestRepository.findByGuid(any())).thenReturn(null);

        mvc.perform(delete(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void xPay_givenRequestAlreadyRefunded_shouldReturn200() throws Exception {

        MultiValueMap<String, String> xPayResumeRequest = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(xPayResumeRequest.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);
        entity.setIsRefunded(true);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        mvc.perform(delete(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void xPay_givenGoodRequestIdForRefund_shouldReturn200() throws Exception {

        MultiValueMap<String, String> xPayResumeRequest = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(xPayResumeRequest.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        XPayOrderStatusResponse orderStatusResponse = ValidBeans.createXPayOrderStatusResponse(true);

        XPayRevertResponse revertResponse = ValidBeans.createXPayRevertResponse(true);

        when(xpayService.callSituazioneOrdine(any())).thenReturn(orderStatusResponse);

        when(xpayService.callStorna(any())).thenReturn(revertResponse);

        mvc.perform(delete(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void xPay_givenGoodRequestIdForRefund_shouldReturn200ButKO() throws Exception {

        MultiValueMap<String, String> xPayResumeRequest = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(xPayResumeRequest.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        XPayOrderStatusResponse orderStatusResponse = ValidBeans.createXPayOrderStatusResponse(true);

        XPayRevertResponse revertResponse = ValidBeans.createXPayRevertResponse(false);

        when(xpayService.callSituazioneOrdine(any())).thenReturn(orderStatusResponse);

        when(xpayService.callStorna(any())).thenReturn(revertResponse);

        mvc.perform(delete(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void xPay_givenGoodRequestIdForRefund_shouldThrownExceptionDurigExecuteXPayOrderStatus() throws Exception {

        MultiValueMap<String, String> xPayResumeRequest = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(xPayResumeRequest.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        when(xpayService.callSituazioneOrdine(any())).thenThrow(RuntimeException.class);


        mvc.perform(delete(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void xPay_givenGoodRequestIdForRefund_shouldThrownExceptionDurigExecuteXPayRevert() throws Exception {

        MultiValueMap<String, String> xPayResumeRequest = ValidBeans.createXPayResumeRequest(true);
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        PaymentRequestEntity entity = ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true);

        AuthPaymentXPayRequest authPaymentXPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        authPaymentXPayRequest.setMac(String.valueOf(xPayResumeRequest.get(XPAY_MAC)));
        String jsonRequest = mapper.writeValueAsString(authPaymentXPayRequest);
        entity.setJsonRequest(jsonRequest);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);

        XPayOrderStatusResponse orderStatusResponse = ValidBeans.createXPayOrderStatusResponse(true);
        when(xpayService.callSituazioneOrdine(any())).thenReturn(orderStatusResponse);

        when(xpayService.callStorna(any())).thenThrow(RuntimeException.class);

        mvc.perform(delete(REQUEST_PAYMENTS_XPAY + "/" + UUID_SAMPLE)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
