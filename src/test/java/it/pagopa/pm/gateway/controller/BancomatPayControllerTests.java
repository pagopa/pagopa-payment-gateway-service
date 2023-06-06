package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.ObjectFactory;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.constant.ApiPaths;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayPaymentRequest;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayRefundRequest;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import it.pagopa.pm.gateway.repository.BPayPaymentResponseRepository;
import it.pagopa.pm.gateway.ExceptionUtil.ExceptionEnumMatcher;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.NestedServletException;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BancomatPayPaymentTransactionsController.class)
@AutoConfigureMockMvc
@EnableWebMvc

public class BancomatPayControllerTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BPayPaymentResponseRepository bPayPaymentResponseRepository;

    @MockBean
    private RestapiCdClientImpl restapiCdClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @MockBean
    private BancomatPayClient client;

    @Mock
    WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

    @Mock
    ObjectFactory objectFactory = new ObjectFactory();

    @Test
    public void givenBancomatPayPaymentRequest_returnBPayPaymentResponseEntity() throws Exception {
        final UUID uuid = UUID.fromString("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(uuid);
            BPayPaymentRequest request = ValidBeans.bPayPaymentRequest();
            given(client.sendPaymentRequest(any(BPayPaymentRequest.class), anyString())).willReturn(ValidBeans.inserimentoRichiestaPagamentoPagoPaResponse());
            mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_BPAY)
                            .content(mapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(ValidBeans.bPayPaymentOutcomeResponseToReturn())));
            verify(bPayPaymentResponseRepository).findByIdPagoPa(1L);
        }
    }

    @Test
    public void givenIncorrectBpayEndpointUrl_shouldReturnGenericErrorException() {
        BPayPaymentRequest request = ValidBeans.bPayPaymentRequest();
        String guid = "guid";

        when(client.sendPaymentRequest(request, guid)).thenAnswer(invocation -> {
            throw new Exception();
        });
        assertThatThrownBy(() -> mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_BPAY)
                .content(mapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)))
                .hasCause(new RestApiException(ExceptionsEnum.GENERIC_ERROR));
    }

    @Test
    public void givenAuthMessage_returnACKMessage() throws Exception {

        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(ValidBeans.bPayPaymentResponseEntityToFind());

        mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                        .header("X-Correlation-ID", "correlationId")
                        .content(mapper.writeValueAsString(ValidBeans.authMessage(OutcomeEnum.OK)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.ackMessageResponse(OutcomeEnum.OK))));
        verify(bPayPaymentResponseRepository).findByCorrelationId("correlationId");
        verify(bPayPaymentResponseRepository).save(ValidBeans.bPayPaymentResponseEntityToSave_2());
        verify(restapiCdClient).callTransactionUpdate(1L, ValidBeans.transactionUpdateRequest());
    }

    @Test
    public void givenProcessedPaymentResponse_shouldReturnTransactionAlreadyProcessed() throws Exception {
        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED)));
        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(ValidBeans.bPayPaymentResponseEntityToSave_2());

        try {
            mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                    .header("X-Correlation-ID", "correlationId")
                    .content(mapper.writeValueAsString(ValidBeans.authMessage(OutcomeEnum.OK)))
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void givenNotFoundProcessedPaymentResponse_shouldReturnTransactionNotFound() throws Exception {
        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.TRANSACTION_NOT_FOUND)));
        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(null);

        try {
            mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                    .header("X-Correlation-ID", "correlationId")
                    .content(mapper.writeValueAsString(ValidBeans.authMessage(OutcomeEnum.OK)))
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }
    }


    @Test
    public void givenExceptionThrownByClient_shouldReturnGenericError() throws Exception {
        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.GENERIC_ERROR)));
        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(ValidBeans.bPayPaymentResponseEntityToFind());
        doThrow(RuntimeException.class)
                .when(restapiCdClient)
                .callTransactionUpdate(1L, ValidBeans.transactionUpdateRequest());

        try {
            mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                    .header("X-Correlation-ID", "correlationId")
                    .content(mapper.writeValueAsString(ValidBeans.authMessage(OutcomeEnum.OK)))
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }

    }

    @Test
    public void givenFeignExceptionThrownByClient_shouldReturnRestapiCDClientError() throws Exception {
        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR)));
        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(ValidBeans.bPayPaymentResponseEntityToFind());
        doThrow(FeignException.class)
                .when(restapiCdClient)
                .callTransactionUpdate(1L, ValidBeans.transactionUpdateRequest());
        try {
            mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                    .header("X-Correlation-ID", "correlationId")
                    .content(mapper.writeValueAsString(ValidBeans.authMessage(OutcomeEnum.OK)))
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void givenBPayPaymentResponseEntityNotFound_shouldReturnTransactionNotFound() throws Exception {
        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.TRANSACTION_NOT_FOUND)));
        given(bPayPaymentResponseRepository.findByIdPagoPa(1L)).willReturn(null);

        try {
            mvc.perform(post(ApiPaths.REQUEST_REFUNDS_BPAY)
                    .content(mapper.writeValueAsString(ValidBeans.bPayRefundRequest()))
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void givenBPayRefundRequestWithNoReturn_shouldReturnGenericError() throws Exception {
        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.GENERIC_ERROR)));
        given(bPayPaymentResponseRepository.findByIdPagoPa(1L)).willReturn(ValidBeans.bPayPaymentResponseEntityToSave());
        given(client.sendInquiryRequest(any(BPayRefundRequest.class), anyString())).willReturn(ValidBeans.inquiryTransactionStatusResponse(false));

        try {
            mvc.perform(post(ApiPaths.REQUEST_REFUNDS_BPAY)
                    .content(mapper.writeValueAsString(ValidBeans.bPayRefundRequest()))
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }
    }


    @Test
    public void givenStornoPagamentoResponseWithNoReturn_shouldReturnGenericError() throws Exception {
        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.GENERIC_ERROR)));
        given(bPayPaymentResponseRepository.findByIdPagoPa(1L)).willReturn(ValidBeans.bPayPaymentResponseEntityToSave());
        given(client.sendInquiryRequest(any(BPayRefundRequest.class), anyString())).willReturn(ValidBeans.inquiryTransactionStatusResponse(true));
        given(client.sendRefundRequest(any(BPayRefundRequest.class), anyString())).willReturn(ValidBeans.stornoPagamentoResponse(false, true));

        try {
            mvc.perform(post(ApiPaths.REQUEST_REFUNDS_BPAY)
                    .content(mapper.writeValueAsString(ValidBeans.bPayRefundRequest()))
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }
    }


    @Test
    public void givenStornoPagamentoResponseWithFalseEsito_shouldReturnGenericError() throws Exception {
        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.GENERIC_ERROR)));
        given(bPayPaymentResponseRepository.findByIdPagoPa(1L)).willReturn(ValidBeans.bPayPaymentResponseEntityToSave());
        given(client.sendInquiryRequest(any(BPayRefundRequest.class), anyString())).willReturn(ValidBeans.inquiryTransactionStatusResponse(true));
        given(client.sendRefundRequest(any(BPayRefundRequest.class), anyString())).willReturn(ValidBeans.stornoPagamentoResponse(true, false));

        try {
            mvc.perform(post(ApiPaths.REQUEST_REFUNDS_BPAY)
                    .content(mapper.writeValueAsString(ValidBeans.bPayRefundRequest()))
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void whenBlankRequestId_thenReturnGenericError() throws Exception {
        String errorMsg = "transactionId is blank: please specify a valid transactionId";
        mvc.perform(get(ApiPaths.RETRIEVE_BPAY_INFO, StringUtils.SPACE))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.bpayInfoResponse(true, errorMsg))));
    }

    @Test
    public void whenTransactionIdIsNotFound_thenReturnNotFound() throws Exception {
        String requestId = "111111111";
        String errorMsg = "No entity has been found for transactionId " + requestId;
        mvc.perform(get(ApiPaths.RETRIEVE_BPAY_INFO, requestId))
                .andExpect(status().isNotFound())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.bpayInfoResponse(true, errorMsg))));
    }

    @Test
    public void whenTransactionIdIsFound_thenReturnBpayInfoResponse() throws Exception {
        when(bPayPaymentResponseRepository.findByIdPagoPa(anyLong()))
                .thenReturn(ValidBeans.bPayPaymentResponseEntityToFind());
        mvc.perform(get(ApiPaths.RETRIEVE_BPAY_INFO, "111111111"))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.bpayInfoResponse(false, null))));
    }

}