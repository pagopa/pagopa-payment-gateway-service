package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.ObjectFactory;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.constant.ApiPaths;
import it.pagopa.pm.gateway.dto.BPayPaymentRequest;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import it.pagopa.pm.gateway.repository.BPayPaymentResponseRepository;
import org.junit.Test;
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
import org.springframework.ws.client.core.WebServiceTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PaymentTransactionsController.class)
@AutoConfigureMockMvc
@EnableWebMvc

public class ControllerTests {

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
                    .andExpect(content().json(mapper.writeValueAsString(ValidBeans.bPayPaymentResponseEntityToReturn())));
            verify(bPayPaymentResponseRepository).findByIdPagoPa(1L);
            verify(bPayPaymentResponseRepository).save(ValidBeans.bPayPaymentResponseEntityToSave());
            verify(client).sendPaymentRequest(request, "null-null-null-null-null");
        }
    }

    @Test
    public void givenIncorrectBpayEndpointUrl_shouldReturnGenericErrorException(){
        BPayPaymentRequest request = ValidBeans.bPayPaymentRequest();
        String guid = "guid";

        when(client.sendPaymentRequest(request, guid)).thenAnswer(invocation -> {throw new Exception();});
        assertThatThrownBy(() -> mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_BPAY)
                .content(mapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)))
               .hasCause(new RestApiException(ExceptionsEnum.GENERIC_ERROR));
    }

    @Test
    public void givenAuthMessage_returnACKMessage() throws Exception {

        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(ValidBeans.bPayPaymentResponseEntityToFind());
        doNothing().when(restapiCdClient).callTransactionUpdate(1L, ValidBeans.transactionUpdateRequest());

        mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                .header("X-Correlation-ID", "correlationId")
                .content(mapper.writeValueAsString(ValidBeans.authMessage()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.ackMessageResponse())));
        verify(bPayPaymentResponseRepository).save(ValidBeans.bPayPaymentResponseEntityToSave_2());
        verify(restapiCdClient).callTransactionUpdate(1L, ValidBeans.transactionUpdateRequest());
    }

    @Test
    public void givenProcessedPaymentResponse_shouldReturnTransactionAlreadyProcessed(){

        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(ValidBeans.bPayPaymentResponseEntityToSave_2());
        doNothing().when(restapiCdClient).callTransactionUpdate(1L, ValidBeans.transactionUpdateRequest());
        assertThatThrownBy(() -> mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                .header("X-Correlation-ID", "correlationId")
                .content(mapper.writeValueAsString(ValidBeans.authMessage()))
                .contentType(MediaType.APPLICATION_JSON)))
                .hasCause(new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED));
    }

    @Test
    public void givenNotFoundProcessedPaymentResponse_shouldReturnTransactionNotFound(){

        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(null);
        doNothing().when(restapiCdClient).callTransactionUpdate(1L, ValidBeans.transactionUpdateRequest());
        assertThatThrownBy(() -> mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                .header("X-Correlation-ID", "correlationId")
                .content(mapper.writeValueAsString(ValidBeans.authMessage()))
                .contentType(MediaType.APPLICATION_JSON)))
                .hasCause(new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND));
    }


    @Test
    public void givenExceptionThrownByClient_shouldReturnGenericError(){

        given(bPayPaymentResponseRepository.findByCorrelationId(anyString())).willReturn(null);
        doThrow(RuntimeException.class)
                .when(restapiCdClient)
                .callTransactionUpdate(1L, ValidBeans.transactionUpdateRequest());

        assertThatThrownBy(() -> mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_BPAY)
                .header("X-Correlation-ID", "correlationId")
                .content(mapper.writeValueAsString(ValidBeans.authMessage()))
                .contentType(MediaType.APPLICATION_JSON)))
                .hasCause(new RestApiException(ExceptionsEnum.GENERIC_ERROR));
    }


}