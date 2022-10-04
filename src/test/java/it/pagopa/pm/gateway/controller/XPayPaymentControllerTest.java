package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.constant.Headers;
import it.pagopa.pm.gateway.dto.XPayAuthRequest;
import it.pagopa.pm.gateway.dto.XPayPollingResponseError;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayRequest;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayResponse;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.repository.PaymentResponseRepository;
import it.pagopa.pm.gateway.service.XpayService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = XPayPaymentController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class XPayPaymentControllerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @MockBean
    private PaymentResponseRepository paymentResponseRepository;
    @MockBean
    private PaymentRequestRepository paymentRequestRepository;
    @MockBean
    private XpayService service;

    @Autowired
    private MockMvc mvc;

    private final String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";

    private static final String APP_ORIGIN = "APP";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void xPay_givenGoodRequest_shouldReturnOkResponse() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        AuthPaymentXPayRequest xPayRequest = ValidBeans.createAuthPaymentRequest(xPayAuthRequest);
        AuthPaymentXPayResponse xPayResponse = ValidBeans.createXPayAuthResponse(xPayRequest);

        when(service.postForObject(any())).thenReturn(xPayResponse);

        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                .content(mapper.writeValueAsString(xPayAuthRequest))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void xPay_givenGoodRequest_shouldThrowResourceAccessEsception() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(service.postForObject(any())).thenThrow(ResourceAccessException.class);

        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void xPay_givenGoodRequest_shouldThrowHttpServerErrorException() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);

        when(service.postForObject(any())).thenThrow(HttpServerErrorException.class);

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
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.xPayAuthResponse(true, BAD_REQUEST_MSG, null))));
    }

    @Test
    public void xPay_givenRequestWithInvalidClientId_shouldReturnBadRequestClientIdResponse() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, "XXX")
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.xPayAuthResponse(true, BAD_REQUEST_MSG_CLIENT_ID, null))));
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
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.xPayAuthResponse(true, TRANSACTION_ALREADY_PROCESSED_MSG, null))));
    }

    @Test
    public void xPay_givenResponseNull_shouldThrowException() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);

        when(service.postForObject(any())).thenReturn(null);

        mvc.perform(post(REQUEST_PAYMENTS_XPAY)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(xPayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void xPay_shouldReturnAuthPollingResponseOK() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(paymentRequestRepository.findByGuid(UUID_SAMPLE))
                .thenReturn(ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, true));

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE ))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.createXpayAuthPollingResponse(true, null))));
    }

    @Test
    public void xPay_shouldReturnAuthPollingResponsePENDING() throws  Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(paymentRequestRepository.findByGuid(UUID_SAMPLE))
                .thenReturn(ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, null));

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.createXpayAuthPollingResponse(false, null))));
    }

    @Test
    public void xPay_shouldReturnAuthPollingResponseKO() throws  Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(paymentRequestRepository.findByGuid(UUID_SAMPLE))
                .thenReturn(ValidBeans.paymentRequestEntityxPay(xPayAuthRequest, APP_ORIGIN, false));

        XPayPollingResponseError error = new XPayPollingResponseError("00", "errorTest");

        when(paymentResponseRepository.findByRequestId(any()))
                .thenReturn(ValidBeans.paymentResponseEntityError(error));

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.createXpayAuthPollingResponse(false, error))));
    }

    @Test
    public void xPAy_givenRequestEntityWithoutHtml_shouldReturnKO() throws Exception {
        XPayAuthRequest xPayAuthRequest = ValidBeans.createXPayAuthRequest(true);
        when(paymentRequestRepository.findByGuid(UUID_SAMPLE))
                .thenReturn(ValidBeans.paymentRequestEntityxPayWithoutHtml(xPayAuthRequest, APP_ORIGIN, true));

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void xPay_givenInvalidRequestId_shouldReturnNotFound() throws Exception {
        when(paymentRequestRepository.findByGuid(any())).thenReturn(null);

        String url = REQUEST_PAYMENTS_XPAY + XPAY_AUTH;
        mvc.perform(get(url, UUID_SAMPLE))
                .andExpect(status().isNotFound());

    }

}
