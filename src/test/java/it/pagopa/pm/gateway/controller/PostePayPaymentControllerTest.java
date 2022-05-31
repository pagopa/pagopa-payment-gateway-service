package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.azure.AzureLoginClient;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.constant.ApiPaths;
import it.pagopa.pm.gateway.dto.BPayPaymentRequest;
import it.pagopa.pm.gateway.dto.PostePayAuthRequest;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.openapitools.client.api.PaymentManagerControllerApi;
import org.openapitools.client.model.CreatePaymentRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PostePayPaymentTransactionsController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class PostePayPaymentControllerTest {


    private static final String EURO_ISO_CODE = "978";
    private static final String BAD_REQUEST_MSG = "Bad Request - mandatory parameters missing";
    private static final String BAD_REQUEST_MSG_CLIENT_ID = "Bad Request - client id is not valid";
    private static final String TRANSACTION_ALREADY_PROCESSED_MSG = "Transaction already processed";
    private static final String SERIALIZATION_ERROR_MSG = "Error while creating json from PostePayAuthRequest object";

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


    @Test
    public void givenPostePayPaymentRequest_returnPostePayAuthResponse() throws Exception {

        final UUID uuid = UUID.fromString("8d8b30e3-de52-4f1c-a71c-9905a8043dac");

        MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class);
        mockedUuid.when(UUID::randomUUID).thenReturn(uuid);

        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);
        CreatePaymentRequest createPaymentRequest = ValidBeans.createPaymentRequest();

        MicrosoftAzureLoginResponse azureLoginResponse = ValidBeans.microsoftAzureLoginResponse();
        String authorizationBearer = "Bearer " + azureLoginResponse.getAccess_token();

        given(azureLoginClient.requestMicrosoftAzureLoginPostepay()).willReturn(ValidBeans.microsoftAzureLoginResponse());
        given(env.getProperty(String.format("postepay.clientId.%s.config", "APP"))).willReturn("1|APP|IMMEDIATA|www.responseurl.com");
        given(postePayControllerApi.apiV1PaymentCreatePost(authorizationBearer,
                createPaymentRequest)).willReturn(ValidBeans.inlineResponse200());

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header("Client-ID", "APP")
                .content(mapper.writeValueAsString(postePayAuthRequest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", false, null))));
        verify(paymentRequestRepository).findByIdTransaction(1L);
        verify(paymentRequestRepository).save(ValidBeans.paymentRequestEntity(postePayAuthRequest));
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
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("XXX", true, BAD_REQUEST_MSG_CLIENT_ID ))));

    }


    @Test
    public void givenRequestWithAlreadyProcessedTransaction_shouldReturnAlreadyProcessedTransactionResponse() throws Exception {
        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);

        given(paymentRequestRepository.findByIdTransaction(1L)).willReturn(ValidBeans.paymentRequestEntity(postePayAuthRequest));

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header("Client-ID", "APP")
                .content(mapper.writeValueAsString(postePayAuthRequest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true, TRANSACTION_ALREADY_PROCESSED_MSG ))));

    }

    @Test
    public void givenRequestWithAlreadyProcejssedTransaction_shouldReturnAlreadyProcessedTransactionResponse() throws Exception {
        PostePayAuthRequest postePayAuthRequest = ValidBeans.postePayAuthRequest(true);

        given(paymentRequestRepository.findByIdTransaction(1L)).willReturn(null);

       /* when(mapper.writeValueAsString(postePayAuthRequest)).thenAnswer(invocation -> {
            throw new JsonProcessingException(SERIALIZATION_ERROR_MSG);
        });
*/
        when(mapper.writeValueAsString(postePayAuthRequest)).thenThrow(JsonProcessingException.class);

        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_POSTEPAY)
                .header("Client-ID", "APP")
                .content(mapper.writeValueAsString(postePayAuthRequest))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.postePayAuthResponse("APP", true, SERIALIZATION_ERROR_MSG ))));



    }



}
