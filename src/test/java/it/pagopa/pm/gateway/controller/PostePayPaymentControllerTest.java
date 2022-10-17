package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.pagopa.pm.gateway.ExceptionUtil.ExceptionEnumMatcher;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.azure.AzureLoginClient;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.constant.ApiPaths;
import it.pagopa.pm.gateway.constant.Headers;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.enums.EndpointEnum;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
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
import org.openapitools.client.model.CreatePaymentResponse;
import org.openapitools.client.model.DetailsPaymentRequest;
import org.openapitools.client.model.RefundPaymentRequest;
import org.openapitools.client.model.RefundPaymentResponse;
import org.openapitools.client.model.DetailsPaymentResponse;
import  org.openapitools.client.model.OnboardingRequest;
import org.openapitools.client.model.OnboardingResponse;

import static it.pagopa.pm.gateway.constant.Params.IS_ONBOARDING_PARAM;
import static org.openapitools.client.model.Esito.APPROVED;
import static org.openapitools.client.model.Esito.DECLINED;
import static org.openapitools.client.model.EsitoStorno.OK;


import org.openapitools.client.api.UserApi;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = PostePayPaymentTransactionsController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class PostePayPaymentControllerTest {

    private static final String APP_CONFIG = "merchantId|shopIdTmp_APP|APP|IMMEDIATA|";
    private static final String WEB_CONFIG = APP_CONFIG + "www.responseurl.com";
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
    private UserApi userApi;

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
            CreatePaymentRequest appRequest = ValidBeans.createPaymentRequest(PaymentChannel.APP);
            CreatePaymentResponse okResponse = ValidBeans.getOkResponse();

            given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
            given(env.getProperty("postepay.clientId.APP.config")).willReturn(APP_CONFIG);
            given(env.getProperty("postepay.logo.url")).willReturn("postepay.png");
            given(postePayControllerApi.apiV1PaymentCreatePost(bearerToken, appRequest)).willReturn(okResponse);

            mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                    .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                    .content(mapper.writeValueAsString(postePayAuthRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse(PaymentChannel.APP.getValue(), false, null))));
            verify(paymentRequestRepository).findByIdTransaction("1");
            verify(paymentRequestRepository).save(ValidBeans.paymentRequestEntity(postePayAuthRequest, null, PaymentChannel.APP.getValue()));
        }
    }

    @Test
    public void givenPostePayPaymentRequestApp_isOnboarding_True_returnPostePayAuthResponse() throws Exception {
        try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(uuid);
            when(uuid.toString()).thenReturn(UUID_SAMPLE);

            PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
            MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
            String bearerToken = "Bearer " + azureLoginResponse.getAccess_token();
            OnboardingRequest onboardingRequest = ValidBeans.createOnboardingRequest(PaymentChannel.APP);
            OnboardingResponse onboardingResponse = ValidBeans.getOKResponseForOnboarding();

            given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
            given(env.getProperty("postepay.clientId.APP.config")).willReturn(APP_CONFIG);
            given(env.getProperty("postepay.logo.url")).willReturn("postepay.png");
            given(userApi.apiV1UserOnboardingPost(bearerToken, onboardingRequest)).willReturn(onboardingResponse);

            mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                            .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                            .content(mapper.writeValueAsString(postePayAuthRequest))
                            .contentType(MediaType.APPLICATION_JSON)
                            .queryParam(IS_ONBOARDING_PARAM, "true"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse(PaymentChannel.APP.getValue(), false, null))));
            verify(paymentRequestRepository).findByIdTransaction("1");

            PostePayOnboardingRequest postePayOnboardingRequest = ValidBeans.createPostePayOnboardingRequest("1");

            PaymentRequestEntity paymentRequestEntity = ValidBeans.paymentRequestEntityOnboarding(postePayOnboardingRequest, null, PaymentChannel.APP.getValue());
            verify(paymentRequestRepository).save(paymentRequestEntity);
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
            String appConfigurationProperty = APP_CONFIG;
            CreatePaymentResponse okResponse = ValidBeans.getOkResponse();


            given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(ValidBeans.microsoftAzureLoginResponse());
            given(env.getProperty(String.format("postepay.clientId.%s.config", "WEB"))).willReturn(appConfigurationProperty);
            given(env.getProperty("postepay.logo.url")).willReturn("postepay.png");
            given(postePayControllerApi.apiV1PaymentCreatePost(bearerToken, request)).willReturn(okResponse);

            mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY).header(Headers.X_CLIENT_ID, "WEB")
                    .content(mapper.writeValueAsString(postePayAuthRequest)).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("WEB", false, null))));
            verify(paymentRequestRepository).findByIdTransaction("1");
            verify(paymentRequestRepository).save(ValidBeans.paymentRequestEntity(postePayAuthRequest, null, "WEB"));
        }
    }

    @Test
    public void givenRequestWithNoIdTransaction_shouldReturnBadRequestResponse() throws Exception {
        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(false);
        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header(Headers.X_CLIENT_ID, "APP")
                .content(mapper.writeValueAsString(postePayAuthRequest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true, BAD_REQUEST_MSG))));
    }

    @Test
    public void givenRequestWithInvalidClientId_shouldReturnBadRequestClientIdResponse() throws Exception {
        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header(Headers.X_CLIENT_ID, "XXX")
                .content(mapper.writeValueAsString(postePayAuthRequest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("XXX", true, BAD_REQUEST_MSG_CLIENT_ID))));
    }

    @Test
    public void givenRequestWithAlreadyProcessedTransaction_shouldReturnAlreadyProcessedTransactionResponse() throws Exception {
        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);

        given(paymentRequestRepository.findByIdTransaction("1")).
                willReturn(ValidBeans.paymentRequestEntity(postePayAuthRequest, null, "APP"));

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header(Headers.X_CLIENT_ID, "APP")
                .content(mapper.writeValueAsString(postePayAuthRequest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true, TRANSACTION_ALREADY_PROCESSED_MSG))));

    }

    @Test
    public void givenPostePayClientResponseNull_shouldReturnExecutingPaymentErrorMsgResponse() throws Exception {

        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
        CreatePaymentRequest createPaymentRequest = ValidBeans.createPaymentRequest(PaymentChannel.APP);
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse = ValidBeans.microsoftAzureLoginResponse();

        given(paymentRequestRepository.findByIdTransaction("1")).willReturn(null);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(microsoftAzureLoginResponse);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);

        given(postePayControllerApi.apiV1PaymentCreatePost(microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest))
                .willReturn(null);

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header(Headers.X_CLIENT_ID, "APP")
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

        given(paymentRequestRepository.findByIdTransaction("1")).willReturn(null);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(microsoftAzureLoginResponse);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);

        doThrow(ApiException.class)
                .when(postePayControllerApi)
                .apiV1PaymentCreatePost(microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest);

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header(Headers.X_CLIENT_ID, "APP")
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

        given(paymentRequestRepository.findByIdTransaction("1")).willReturn(null);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(microsoftAzureLoginResponse);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);

        doThrow(RuntimeException.class)
                .when(postePayControllerApi)
                .apiV1PaymentCreatePost(microsoftAzureLoginResponse.getAccess_token(), createPaymentRequest);

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header(Headers.X_CLIENT_ID, "APP")
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
        given(env.getProperty("postepay.pgs.response.APP.clientResponseUrl.payment")).willReturn("www.clientResponseUrl.com");

        mvc.perform(get(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayPollingResponse())));
    }

    @Test
    public void givenPaymentRequestEntityWithNoAuthOutcome_shouldReturnPollingResponseError() throws Exception {

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).
                willReturn(ValidBeans.paymentRequestEntity(null, null, "APP"));

        mvc.perform(get(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.
                        postePayPollingResponseError("No authorization outcome has been received yet", null))));

    }

    @Test
    public void givenPaymentRequestEntityWithKOAuthOutcome_shouldReturnPollingResponseError() throws Exception {

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).
                willReturn(ValidBeans.paymentRequestEntity(null, false, "APP"));

        mvc.perform(get(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH, UUID_SAMPLE))
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.
                        postePayPollingResponseError("Payment authorization has not been granted", OutcomeEnum.KO))));
    }

    @Test
    public void givenNotFoundPaymentResponseEntity_shouldThrowTransactionNotFoundException() throws Exception {

        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.TRANSACTION_NOT_FOUND)));

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).
                willReturn(null);
        try {
            mvc.perform(get(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH, UUID_SAMPLE));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }

    }

    @Test
    public void givenAuthMessage_shouldReturnACKMessage() throws Exception {

        AuthMessage authMessage = ValidBeans.authMessage(OutcomeEnum.OK);
        ACKMessage ackMessage = ValidBeans.ackMessageResponse(OutcomeEnum.OK);
        final String correlationID = "correlation-ID";

        PaymentRequestEntity paymentRequestEntity = ValidBeans.paymentRequestEntity(null, true, "APP");
        PatchRequest patchRequest = ValidBeans.patchRequest();

        given(paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(correlationID, EndpointEnum.POSTEPAY.getValue())).willReturn(paymentRequestEntity);
        given(restapiCdClient.callPatchTransactionV2(Long.valueOf(paymentRequestEntity.getIdTransaction()), patchRequest))
                .willReturn(OutcomeEnum.OK.toString());

        mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header("X-Correlation-ID", correlationID)
                .content(mapper.writeValueAsString(authMessage))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ackMessage)));
        verify(paymentRequestRepository).save(paymentRequestEntity);
    }

    @Test
    public void givenAlreadyProcessedPaymentResponseEntity_shouldReturnTransactionAlreadyProcessedException() throws Exception {

        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED)));

        AuthMessage authMessage = ValidBeans.authMessage(OutcomeEnum.KO);
        ACKMessage ackMessage = ValidBeans.ackMessageResponse(OutcomeEnum.OK);
        final String correlationID = "correlation-ID";
        PaymentRequestEntity paymentRequestEntity = ValidBeans.paymentRequestEntity(null, true, "APP");
        paymentRequestEntity.setIsProcessed(true);

        given(paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(correlationID, EndpointEnum.POSTEPAY.getValue())).willReturn(paymentRequestEntity);

        try {
            mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                    .header("X-Correlation-ID", correlationID)
                    .content(mapper.writeValueAsString(authMessage))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(ackMessage)));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }

    }

    @Test
    public void givenNotFoundPaymentResponseEntity_shouldReturnTransactionNotFoundException() throws Exception {

        thrown.expect(ExceptionEnumMatcher.withExceptionEnum(equalTo(ExceptionsEnum.TRANSACTION_NOT_FOUND)));

        AuthMessage authMessage = ValidBeans.authMessage(OutcomeEnum.OK);
        ACKMessage ackMessage = ValidBeans.ackMessageResponse(OutcomeEnum.OK);
        final String correlationID = "correlation-ID";

        given(paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(correlationID, EndpointEnum.POSTEPAY.getValue())).willReturn(null);

        try {
            mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                    .header("X-Correlation-ID", correlationID)
                    .content(mapper.writeValueAsString(authMessage))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(ackMessage)));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }

    }

    @Test
    public void shouldReturnKOMessageResponse() throws Exception {

        AuthMessage authMessage = ValidBeans.authMessage(OutcomeEnum.OK);
        ACKMessage ackMessage = ValidBeans.ackMessageResponse(OutcomeEnum.KO);
        final String correlationID = "correlation-ID";
        PaymentRequestEntity paymentRequestEntity = ValidBeans.paymentRequestEntityOnboardingFalse(null, true, "APP");
        PatchRequest patchRequest = ValidBeans.patchRequest();

        doThrow(FeignException.class)
                .when(restapiCdClient)
                .callPatchTransactionV2(Long.valueOf(paymentRequestEntity.getIdTransaction()), patchRequest);

        given(paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(correlationID, EndpointEnum.POSTEPAY.getValue())).willReturn(paymentRequestEntity);

        try {
            mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                    .header("X-Correlation-ID", correlationID)
                    .content(mapper.writeValueAsString(authMessage))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().json(mapper.writeValueAsString(ackMessage)));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }

    }

    @Test
    public void thrownException_shouldReturnGenericErrorException() throws Exception {

        AuthMessage authMessage = ValidBeans.authMessage(OutcomeEnum.OK);
        ACKMessage ackMessage = ValidBeans.ackMessageResponse(OutcomeEnum.KO);
        final String correlationID = "correlation-ID";
        PaymentRequestEntity paymentRequestEntity = ValidBeans.paymentRequestEntityOnboardingFalse(null, true, "APP");
        PatchRequest patchRequest = ValidBeans.patchRequest();

        doThrow(RuntimeException.class)
                .when(restapiCdClient)
                .callPatchTransactionV2(Long.valueOf(paymentRequestEntity.getIdTransaction()), patchRequest);

        given(paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(correlationID, EndpointEnum.POSTEPAY.getValue())).willReturn(paymentRequestEntity);

        try {
            mvc.perform(put(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                    .header("X-Correlation-ID", correlationID)
                    .content(mapper.writeValueAsString(authMessage))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().json(mapper.writeValueAsString(ackMessage)));
        } catch (NestedServletException | JsonProcessingException e) {
            throw (Exception) e.getCause();
        }

    }

    @Test
    public void givenRequestId_executeRefund() throws Exception {
        PaymentRequestEntity paymentRequestEntity = ValidBeans.paymentRequestEntityWithRefundData("APP", "auth_code", false, false);
        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
        RefundPaymentRequest refundPaymentRequest = ValidBeans.refundPaymentRequest("auth_code");

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
        given(postePayControllerApi.apiV1PaymentRefundPost("Bearer " + azureLoginResponse.getAccess_token(), refundPaymentRequest))
                .willReturn(ValidBeans.refundPaymentResponse(OK));

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH, UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", "OK", null))));
        verify(paymentRequestRepository).save(paymentRequestEntity);
    }

    @Test
    public void givenInvalidRequestId_shouldReturn404PostePayRefundResponse() throws Exception {

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(null);

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH, UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, null, null, "Payment request not found"))));
    }


    @Test
    public void givenInvalidRequestEndPoint_shouldReturn404PaymentNotFound() throws Exception {
        PaymentRequestEntity paymentRequestEntity = ValidBeans.paymentRequestEntityWithRefundData("APP", "auth_code", false, true);

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH, UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, null, null, "Payment request not found"))));
    }


    @Test
    public void givenRefundedTransaction_shouldReturn200AlreadyProcessed() throws Exception {
        PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", null, true, false);

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", null, "Refund request already processed"))));
    }


    @Test
    public void givenDeclinedDetailsCheck_shouldReturn200RefundNotAuthorized() throws Exception {
        PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", null, false, false);
        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();

        DetailsPaymentRequest detailsPaymentRequest = ValidBeans.detailsPaymentRequest();
        DetailsPaymentResponse detailsPaymentResponse = ValidBeans.detailsPaymentResponse(DECLINED);
        String authorization = "Bearer " + azureLoginResponse.getAccess_token();

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
        given(postePayControllerApi.apiV1PaymentDetailsPost(authorization, detailsPaymentRequest))
                .willReturn(detailsPaymentResponse);

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", null, "Transaction is not refundable: authorization has not been approved by PostePay or has been refunded already"))));
    }


    @Test
    public void thrownApiExceptionByAzuteLogin_return500PostePayServiceException() throws Exception {

        PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", null, false, false);
        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);

        doThrow(RuntimeException.class)
                .when(azureLoginClient)
                .requestMicrosoftAzureLoginPostepay();

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", null, "Exception during call to PostePay service"))));
    }



    @Test
    public void thrownApiExceptionByCheckDetails_executeRefund() throws Exception {

       PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", null, false, false);
        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();

        RefundPaymentRequest refundPaymentRequest = ValidBeans.refundPaymentRequest(null);
        DetailsPaymentRequest detailsPaymentRequest = ValidBeans.detailsPaymentRequest();
        String authorization = "Bearer " + azureLoginResponse.getAccess_token();

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
        given(postePayControllerApi.apiV1PaymentRefundPost(authorization, refundPaymentRequest))
                .willReturn(ValidBeans.refundPaymentResponse(OK));

        doThrow(ApiException.class)
                .when(postePayControllerApi)
                .apiV1PaymentDetailsPost(authorization, detailsPaymentRequest);

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", "OK", null))));
        verify(paymentRequestRepository).save(paymentRequestEntity);
    }

    @Test
    public void checkDetailsResponseIsNull_executeRefund() throws Exception {

        PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", null, false, false);

        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
         RefundPaymentRequest refundPaymentRequest = ValidBeans.refundPaymentRequest(null);
        DetailsPaymentRequest detailsPaymentRequest = ValidBeans.detailsPaymentRequest();
        String authorization = "Bearer " + azureLoginResponse.getAccess_token();


        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
        given(postePayControllerApi.apiV1PaymentDetailsPost(authorization, detailsPaymentRequest))
                .willReturn( null);
        given(postePayControllerApi.apiV1PaymentRefundPost(authorization, refundPaymentRequest))
                .willReturn(ValidBeans.refundPaymentResponse(OK));


        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", "OK", null))));
        verify(paymentRequestRepository).save(paymentRequestEntity);
    }

    @Test
    public void checkDetailsResponseEsitoIsNull_executeRefund() throws Exception {

        PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", null, false, false);

        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
        RefundPaymentRequest refundPaymentRequest = ValidBeans.refundPaymentRequest(null);
        DetailsPaymentRequest detailsPaymentRequest = ValidBeans.detailsPaymentRequest();
        DetailsPaymentResponse detailsPaymentResponse = ValidBeans.detailsPaymentResponse(null);

        String authorization = "Bearer " + azureLoginResponse.getAccess_token();

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
        given(postePayControllerApi.apiV1PaymentDetailsPost(authorization, detailsPaymentRequest))
                .willReturn( detailsPaymentResponse);
        given(postePayControllerApi.apiV1PaymentRefundPost(authorization, refundPaymentRequest))
                .willReturn(ValidBeans.refundPaymentResponse(OK));


        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", "OK", null))));
        verify(paymentRequestRepository).save(paymentRequestEntity);
    }


    @Test
    public void refundResponseIsNull_shouldReturn500PostePayServiceException() throws Exception {
        PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", "auth_code", false, false);
        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
        RefundPaymentRequest refundPaymentRequest = ValidBeans.refundPaymentRequest("auth_code");
        String authorization = "Bearer " + azureLoginResponse.getAccess_token();

        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
        given(postePayControllerApi.apiV1PaymentRefundPost(authorization, refundPaymentRequest))
                .willReturn(null);

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", null, "Exception during call to PostePay service"))));
    }


    @Test
    public void refundResponseEsitoStornoIsNull_shouldReturn500PostePayServiceException() throws Exception {
        PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", "auth_code", false, false);
        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
        RefundPaymentRequest refundPaymentRequest = ValidBeans.refundPaymentRequest("auth_code");
        DetailsPaymentRequest detailsPaymentRequest = ValidBeans.detailsPaymentRequest();
        DetailsPaymentResponse detailsPaymentResponse = ValidBeans.detailsPaymentResponse(APPROVED);
        RefundPaymentResponse refundPaymentResponse = ValidBeans.refundPaymentResponse(null);
        String authorization = "Bearer " + azureLoginResponse.getAccess_token();


        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
        given(postePayControllerApi.apiV1PaymentDetailsPost(authorization, detailsPaymentRequest))
                .willReturn( detailsPaymentResponse);
        given(postePayControllerApi.apiV1PaymentRefundPost(authorization, refundPaymentRequest))
                .willReturn(refundPaymentResponse);

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", null, "Exception during call to PostePay service"))));
    }


    @Test
    public void thrownApiExceptionByRefundPost_shouldReturn500PostePayServiceException() throws Exception {

        PaymentRequestEntity paymentRequestEntity =ValidBeans.paymentRequestEntityWithRefundData("APP", "auth_code", false, false);
        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
        RefundPaymentRequest refundPaymentRequest = ValidBeans.refundPaymentRequest("auth_code");
        DetailsPaymentRequest detailsPaymentRequest = ValidBeans.detailsPaymentRequest();
        String authorization = "Bearer " + azureLoginResponse.getAccess_token();


        given(paymentRequestRepository.findByGuid(UUID_SAMPLE)).willReturn(paymentRequestEntity);
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn(WEB_CONFIG);
        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(azureLoginResponse);
        given(postePayControllerApi.apiV1PaymentDetailsPost(authorization, detailsPaymentRequest))
                .willReturn( null);

        doThrow(ApiException.class)
                .when(postePayControllerApi)
                .apiV1PaymentRefundPost(authorization, refundPaymentRequest);

        mvc.perform(delete(ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH,UUID_SAMPLE)
                .header(Headers.X_CLIENT_ID, PaymentChannel.APP.getValue())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayRefundResponse(UUID_SAMPLE, "1234", null, "Exception during call to PostePay service"))));

    }


}