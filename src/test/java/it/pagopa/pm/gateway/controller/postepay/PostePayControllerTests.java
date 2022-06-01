package it.pagopa.pm.gateway.controller.postepay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import it.pagopa.pm.gateway.ExceptionUtil.ExceptionEnumMatcher;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.constant.ApiPaths;
import it.pagopa.pm.gateway.dto.enums.EndpointEnum;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.NestedServletException;

import java.nio.charset.Charset;
import java.util.HashMap;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PostePayPaymentTransactionsController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class PostePayControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PaymentRequestRepository paymentRequestRepository;
    @MockBean
    private RestapiCdClientImpl restapiCdClient;
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void givenAuthMessage_returnACKMessage() throws Exception {
        given(paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(anyString(), anyString())).willReturn(
                ValidBeans.pPayPaymentRequestEntityToFind());

        given(restapiCdClient.callClosePayment(1L, true, "authCode")).willReturn(
                OutcomeEnum.OK.name());

        mockMvc.perform(put(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("X-Correlation-ID", "correlationId")
                        .content(objectMapper.writeValueAsString(ValidBeans.authMessage()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(ValidBeans.ackMessageResponse())));
        verify(paymentRequestRepository).findByCorrelationIdAndRequestEndpoint("correlationId",
                EndpointEnum.POSTEPAY.getValue());
        verify(paymentRequestRepository).save(ValidBeans.pPayPaymentRequestEntityToSave());
    }

    @Test
    public void givenExceptionThrownByClient_shouldReturnGenericError() throws Exception {
        given(paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(anyString(), anyString())).willReturn(
                ValidBeans.pPayPaymentRequestEntityToFind());

        given(restapiCdClient.callClosePayment(1L, true, "authCode")).willReturn(
                OutcomeEnum.KO.name());

        mockMvc.perform(put(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("X-Correlation-ID", "correlationId")
                        .content(objectMapper.writeValueAsString(ValidBeans.authMessage()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(ValidBeans.ackGenericErrorResponse())));
        verify(paymentRequestRepository).findByCorrelationIdAndRequestEndpoint("correlationId",
                EndpointEnum.POSTEPAY.getValue());
    }

    @Test
    public void givenFeignExceptionThrownByClient_shouldReturnRestapiCDClientError() throws Exception {
        given(paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(anyString(), anyString())).willReturn(
                ValidBeans.pPayPaymentRequestEntityToFind());

        doThrow(FeignException.class)
                .when(restapiCdClient)
                .callClosePayment(1L, true, "authCode");

        mockMvc.perform(put(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("X-Correlation-ID", "correlationId")
                        .content(objectMapper.writeValueAsString(ValidBeans.authMessage()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(ValidBeans.ackRestapiCdClientErrorResponse())));
        verify(paymentRequestRepository).findByCorrelationIdAndRequestEndpoint("correlationId",
                EndpointEnum.POSTEPAY.getValue());
    }

}
