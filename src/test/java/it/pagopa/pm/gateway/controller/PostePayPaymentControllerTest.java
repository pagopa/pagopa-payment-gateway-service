package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.ExceptionUtil.ExceptionEnumMatcher;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.azure.AzureLoginClient;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.constant.ApiPaths;
import it.pagopa.pm.gateway.constant.Headers;
import it.pagopa.pm.gateway.dto.PostePayAuthRequest;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.PaymentManagerControllerApi;
import org.openapitools.client.model.CreatePaymentRequest;
import org.openapitools.client.model.InlineResponse200;
import org.openapitools.client.model.PaymentChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.NestedServletException;

import java.util.UUID;

import static it.pagopa.pm.gateway.constant.Messages.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = PostePayPaymentTransactionsController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class PostePayPaymentControllerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @MockBean
    AzureLoginClient azureLoginClient;

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @MockBean
    PaymentRequestRepository paymentRequestRepository;

    @MockBean
    private RestapiCdClientImpl restapiCdClient;

    @MockBean
    private PaymentManagerControllerApi postePayControllerApi;

    @MockBean
    private Environment env;

    private final String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";

    @Mock
    final UUID uuid = UUID.fromString(UUID_SAMPLE);

    @Test
    public void givenPostePayPaymentRequestAPP_returnPostePayAuthResponse() throws Exception {
        try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(uuid);
            when(uuid.toString()).thenReturn(UUID_SAMPLE);

            PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
            MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
            String bearerToken = "Bearer " + azureLoginResponse.getAccess_token();
            CreatePaymentRequest request = ValidBeans.createPaymentRequest(PaymentChannel.APP);
            String appConfigurationProperty = "shopIdTmp_APP|APP|IMMEDIATA|";
            InlineResponse200 okResponse = ValidBeans.getOkResponse();

            given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
            given(env.getProperty(String.format("postepay.clientId.%s.config", PaymentChannel.APP.getValue()))).willReturn(appConfigurationProperty);
            given(postePayControllerApi.apiV1PaymentCreatePost(bearerToken, request)).willReturn(okResponse);

            mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                            .header(Headers.CLIENT_ID, PaymentChannel.APP.getValue())
                            .content(mapper.writeValueAsString(postePayAuthRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse(PaymentChannel.APP.getValue(), false, null))));
            verify(paymentRequestRepository).findByIdTransaction(1L);
            verify(paymentRequestRepository).save(ValidBeans.paymentRequestEntity(postePayAuthRequest, null, PaymentChannel.APP.getValue()));
        }
    }

   @Test
    public void givenPostePayPaymentRequestWEB_returnPostePayAuthResponse() throws Exception {
        try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(uuid);
            PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
            when(uuid.toString()).thenReturn(UUID_SAMPLE);

            MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
            String bearerToken = "Bearer " + azureLoginResponse.getAccess_token();
            CreatePaymentRequest request = ValidBeans.createPaymentRequest(PaymentChannel.APP);
            String appConfigurationProperty = "shopIdTmp_APP|APP|IMMEDIATA|";
            InlineResponse200 okResponse = ValidBeans.getOkResponse();


            given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(ValidBeans.microsoftAzureLoginResponse());
            given(env.getProperty(String.format("postepay.clientId.%s.config", "WEB"))).willReturn(appConfigurationProperty);
            given(postePayControllerApi.apiV1PaymentCreatePost(bearerToken, request)).willReturn(okResponse);

            mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY).header("Client-ID", "WEB")
                    .content(mapper.writeValueAsString(postePayAuthRequest)).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("WEB", false, null))));
            verify(paymentRequestRepository).findByIdTransaction(1L);
            verify(paymentRequestRepository).save(ValidBeans.paymentRequestEntity(postePayAuthRequest, null, "WEB"));
        }
    }

   @Test
    public void givenRequestWithNoIdTransaction_shouldReturnBadRequestResponse() throws Exception {
        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(false);
        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("Client-ID", "APP")
                        .content(mapper.writeValueAsString(postePayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true, BAD_REQUEST_MSG))));
    }

    @Test
    public void givenRequestWithInvalidClientId_shouldReturnBadRequestClientIdResponse() throws Exception {
        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("Client-ID", "XXX")
                        .content(mapper.writeValueAsString(postePayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("XXX", true, BAD_REQUEST_MSG_CLIENT_ID))));
    }


   @Test
    public void givenRequestWithAlreadyProcessedTransaction_shouldReturnAlreadyProcessedTransactionResponse() throws Exception {
        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);

        given(paymentRequestRepository.findByIdTransaction(1L)).
                willReturn(ValidBeans.paymentRequestEntity(postePayAuthRequest, null, "APP"));

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("Client-ID", "APP")
                        .content(mapper.writeValueAsString(postePayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true, TRANSACTION_ALREADY_PROCESSED_MSG))));

    }


    @Test
    public void givenPostePayClientResponseNull_shouldReturnExecutingPaymentErrorMsgResponse() throws Exception {

        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
        CreatePaymentRequest createPaymentRequest = ValidBeans.createPaymentRequest(PaymentChannel.APP);
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse = ValidBeans.microsoftAzureLoginResponse();

        given(paymentRequestRepository.findByIdTransaction(1L)).willReturn(null);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(microsoftAzureLoginResponse);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn("shopIdTmp_APP|APP|IMMEDIATA|www.responseurl.com");

        given(postePayControllerApi.apiV1PaymentCreatePost(microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest))
                .willReturn(null);

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("Client-ID", "APP")
                        .content(mapper.writeValueAsString(postePayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true,
                        GENERIC_ERROR_MSG + postePayAuthRequest.getIdTransaction()))));
        verify(postePayControllerApi).apiV1PaymentCreatePost("Bearer " + microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest);
    }


    @Test
    public void thrownApiException_shouldReturnExecutingPaymentErrorMsgResponse() throws Exception {

        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
        CreatePaymentRequest createPaymentRequest = ValidBeans.createPaymentRequest(PaymentChannel.APP);
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse = ValidBeans.microsoftAzureLoginResponse();

        given(paymentRequestRepository.findByIdTransaction(1L)).willReturn(null);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(microsoftAzureLoginResponse);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn("shopIdTmp_APP|APP|IMMEDIATA|www.responseurl.com");

        doThrow(ApiException.class)
                .when(postePayControllerApi)
                .apiV1PaymentCreatePost(microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest);

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("Client-ID", "APP")
                        .content(mapper.writeValueAsString(postePayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true,
                        GENERIC_ERROR_MSG + postePayAuthRequest.getIdTransaction()))));
        verify(postePayControllerApi).apiV1PaymentCreatePost("Bearer " + microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest);
    }

    @Test
    public void thrownUncheckedException_shouldReturnExecutingPaymentErrorMsgResponse() throws Exception {

        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
        CreatePaymentRequest createPaymentRequest = ValidBeans.createPaymentRequest(PaymentChannel.APP);
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse = ValidBeans.microsoftAzureLoginResponse();

        given(paymentRequestRepository.findByIdTransaction(1L)).willReturn(null);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(microsoftAzureLoginResponse);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn("shopIdTmp_APP|APP|IMMEDIATA|www.responseurl.com");

        doThrow(RuntimeException.class)
                .when(postePayControllerApi)
                .apiV1PaymentCreatePost(microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest);

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                        .header("Client-ID", "APP")
                        .content(mapper.writeValueAsString(postePayAuthRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true,
                        GENERIC_ERROR_MSG + postePayAuthRequest.getIdTransaction()))));
        verify(postePayControllerApi).apiV1PaymentCreatePost("Bearer " + microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest);
    }


    @Test
    public void shouldReturnPollingResponseOK() throws Exception {

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).
                willReturn(ValidBeans.paymentRequestEntity(null, true, "APP"));

        mvc.perform(get(ApiPaths.REQUEST_PAYMENTS_POSTEPAY_REQUEST_ID, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayPollingResponse())));
    }


    @Test
    public void givenNotFoundPaymentResponseEntity_shouldThrowTransactionNotFoundException() throws Exception {

        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.TRANSACTION_NOT_FOUND)));

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).
                willReturn(null);
        try {
            mvc.perform(get(ApiPaths.REQUEST_PAYMENTS_POSTEPAY_REQUEST_ID, UUID_SAMPLE));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }

    }


}
