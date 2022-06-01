package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.pagopa.pm.gateway.client.azure.AzureLoginClient;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.enums.EndpointEnum;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.PaymentManagerControllerApi;
import org.openapitools.client.model.InlineResponse200;
import org.openapitools.client.model.CreatePaymentRequest;
import org.openapitools.client.model.AuthorizationType;
import org.openapitools.client.model.ResponseURLs;
import org.openapitools.client.model.PaymentChannel;
import org.openapitools.client.model.Error;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.SocketTimeoutException;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_POSTEPAY;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENT_POSTEPAY_REQUEST_ID;
import static it.pagopa.pm.gateway.constant.ClientConfigs.*;
import static it.pagopa.pm.gateway.constant.ClientConfigs.RESPONSE_URL_CONFIG;
import static it.pagopa.pm.gateway.constant.Headers.*;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.KO;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.OK;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
public class PostePayPaymentTransactionsController {

    private static final String EURO_ISO_CODE = "978";
    private static final String BAD_REQUEST_MSG = "Bad Request - mandatory parameters missing";
    private static final String BAD_REQUEST_MSG_CLIENT_ID = "Bad Request - client id is not valid";
    private static final String TRANSACTION_ALREADY_PROCESSED_MSG = "Transaction already processed";
    private static final String SERIALIZATION_ERROR_MSG = "Error while creating json from PostePayAuthRequest object";
    private static final String EXECUTING_PAYMENT_FOR_ID_TRANSACTION_ERROR_MSG = "Error while executing payment for idTransaction ";

    @Value("${postepay.pgs.response.urlredirect}")
    private String PAYMENT_RESPONSE_URLREDIRECT;

    @Value("${postepay.notificationURL}")
    private String POSTEPAY_NOTIFICATION_URL;

    @Autowired
    private AzureLoginClient azureLoginClient;

    @Autowired
    private RestapiCdClientImpl restapiCdClient;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private PaymentManagerControllerApi postePayControllerApi;

    @Autowired
    private Environment env;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final List<String> VALID_CLIENT_ID = Arrays.asList("APP", "WEB");

    @PutMapping(REQUEST_PAYMENTS_POSTEPAY)
    public ACKMessage closePayment(@RequestBody AuthMessage authMessage,
                                   @RequestHeader(X_CORRELATION_ID) String correlationId) throws RestApiException {
        MDC.clear();
        log.info("START Update postepay transaction request for correlation-id: " + correlationId + ": " + authMessage);

        if (Objects.isNull(authMessage) || authMessage.getAuthOutcome() == null || StringUtils.isBlank(correlationId)) {
            throw new RestApiException(ExceptionsEnum.MISSING_FIELDS);
        }

        PaymentRequestEntity postePayPaymentRequest = paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(
                correlationId, EndpointEnum.POSTEPAY.getValue());

        if (postePayPaymentRequest == null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        } else {
            setMdcFields(postePayPaymentRequest.getMdcInfo());
            if (Boolean.TRUE.equals(postePayPaymentRequest.getIsProcessed())) {
                throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
            }
        }

        try {
            boolean isAuthOk = authMessage.getAuthOutcome() == OutcomeEnum.OK;
            String closePayment = restapiCdClient.callClosePayment(postePayPaymentRequest.getIdTransaction(), isAuthOk);
            postePayPaymentRequest.setIsProcessed(true);
            postePayPaymentRequest.setAuthorizationCode(authMessage.getAuthCode());
            postePayPaymentRequest.setAuthorizationOutcome(isAuthOk);
            paymentRequestRepository.save(postePayPaymentRequest);
            log.info("Response from closePayment for correlation-id: " + correlationId + " " + closePayment);
        } catch (FeignException fe) {
            log.error("Exception calling RestapiCD to close payment", fe);
            throw new RestApiException(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR, fe.status());
        } catch (Exception e) {
            log.error("Exception closing payment", e);
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new RestApiException(ExceptionsEnum.TIMEOUT);
            }
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } finally {
            log.info("END Update postepay transaction request for correlation-id: " + correlationId);
        }

        return new ACKMessage(OK);
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENTS_POSTEPAY)
    public ResponseEntity<PostePayAuthResponse> requestPaymentsPostepay(@RequestHeader(value = CLIENT_ID) String clientId,
                                                                        @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                        @RequestBody PostePayAuthRequest postePayAuthRequest) {
        log.info("START - request-payments/postepay");
        setMdcFields(mdcFields);

        if (ObjectUtils.anyNull(postePayAuthRequest, postePayAuthRequest.getIdTransaction(), postePayAuthRequest.getGrandTotal())) {
            log.error("Mandatory request parameters missing");
            return createPostePayAuthResponse(clientId, BAD_REQUEST_MSG, true, HttpStatus.BAD_REQUEST);
        }

        if (!VALID_CLIENT_ID.contains(clientId)) {
            log.error("Client id is not valid " + clientId);
            return createPostePayAuthResponse(clientId, BAD_REQUEST_MSG_CLIENT_ID, true, HttpStatus.BAD_REQUEST);
        }

        Long idTransaction = postePayAuthRequest.getIdTransaction();
        if (Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction))) {
            log.error("Transaction " + idTransaction + " has already been processed previously");
            return createPostePayAuthResponse(clientId, TRANSACTION_ALREADY_PROCESSED_MSG, true, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PaymentRequestEntity paymentRequestEntity;
        try {
            String authRequestJson = OBJECT_MAPPER.writeValueAsString(postePayAuthRequest);
            log.debug("Resulting postePayAuthRequest JSON string = " + authRequestJson);
            paymentRequestEntity = generateRequestEntity(mdcFields, idTransaction);
            paymentRequestEntity.setJsonRequest(authRequestJson);
            log.info("PostePay request object generated");
        } catch (JsonProcessingException e) {
            log.error(SERIALIZATION_ERROR_MSG, e);
            return createPostePayAuthResponse(clientId, SERIALIZATION_ERROR_MSG, true, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            executePostePayPayment(postePayAuthRequest, clientId, paymentRequestEntity);
        } catch (Exception e) {
            return createPostePayAuthResponse(clientId, EXECUTING_PAYMENT_FOR_ID_TRANSACTION_ERROR_MSG + idTransaction, true, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("END requestPaymentsPostepay " + idTransaction);
        return createPostePayAuthResponse(clientId, null, false, HttpStatus.OK);
    }

    private PaymentRequestEntity generateRequestEntity(String mdcFields, Long idTransaction) {
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_POSTEPAY);
        paymentRequestEntity.setIdTransaction(idTransaction);
        paymentRequestEntity.setMdcInfo(mdcFields);
        return paymentRequestEntity;
    }

    @GetMapping(REQUEST_PAYMENT_POSTEPAY_REQUEST_ID)
    @ResponseBody
    public PostePayPollingResponse getPaymentPostepayResponse(@PathVariable String requestId, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws RestApiException {
        setMdcFields(mdcFields);
        PaymentRequestEntity request = paymentRequestRepository.findByGuid(requestId);
        if (request == null || !REQUEST_PAYMENTS_POSTEPAY.equals(request.getRequestEndpoint())) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        }
        return new PostePayPollingResponse(
                request.getClientId(),
                request.getAuthorizationUrl(),
                Boolean.TRUE.equals(request.getAuthorizationOutcome()) ? OK : KO,
                request.getErrorCode()
        );
    }

    @Async
    private void executePostePayPayment(PostePayAuthRequest postePayAuthRequest, String clientId, PaymentRequestEntity paymentRequestEntity)
            throws RestApiException, JsonProcessingException {
        Long idTransaction = postePayAuthRequest.getIdTransaction();
        log.info("START executePostePayPayment for transaction " + idTransaction);

        CreatePaymentRequest createPaymentRequest = mapPostePayAuthRequestToCreatePaymentRequest(postePayAuthRequest, clientId);
        InlineResponse200 inlineResponse200;

        try {
            MicrosoftAzureLoginResponse microsoftAzureLoginResponse = azureLoginClient.requestMicrosoftAzureLoginPostepay();
            String bearerTokenAuthorization = "Bearer " + microsoftAzureLoginResponse.getAccess_token();
            inlineResponse200 = postePayControllerApi.apiV1PaymentCreatePost(bearerTokenAuthorization, createPaymentRequest);
            if (inlineResponse200 == null) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            log.info("Response from PostePay createPayment - idTransaction: " + idTransaction + " - paymentID: "
                    + inlineResponse200.getPaymentID() + " - userRedirectUrl: " + inlineResponse200.getUserRedirectURL());
        } catch (ApiException e) {
            Error error = OBJECT_MAPPER.readValue(e.getResponseBody(), Error.class);
            log.error("Error from PostePay createPayment: " + error);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } catch (Exception e) {
            log.error("Exception while calling Postepay - setting AuthorizationOutcome to false - idTransaction " + idTransaction, e);
            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("SocketTimeoutException during Postepay calling");
                throw new RestApiException(ExceptionsEnum.TIMEOUT);
            }
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        savePaymentRequestEntity(paymentRequestEntity, inlineResponse200.getPaymentID(), inlineResponse200.getUserRedirectURL());
        log.info("END executePostePayPayment for transaction" + idTransaction);
    }

    private void savePaymentRequestEntity(PaymentRequestEntity paymentRequestEntity, String correlationId, String authorizationUrl) {
        paymentRequestEntity.setCorrelationId(correlationId);
        paymentRequestEntity.setAuthorizationUrl(authorizationUrl);
        paymentRequestRepository.save(paymentRequestEntity);
    }

    private CreatePaymentRequest mapPostePayAuthRequestToCreatePaymentRequest(PostePayAuthRequest postePayAuthRequest, String clientId) {

        String configs = env.getProperty(String.format("postepay.clientId.%s.config", clientId));

        Map<String, String> configsMap = getConfigValues(configs);

        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
        createPaymentRequest.setAmount(String.valueOf(postePayAuthRequest.getGrandTotal()));
        createPaymentRequest.setPaymentChannel(PaymentChannel.valueOf(configsMap.get(PAYMENT_CHANNEL_CONFIG)));
        createPaymentRequest.setAuthType(AuthorizationType.fromValue(configsMap.get(AUTH_TYPE_CONFIG)));
        createPaymentRequest.setBuyerEmail(postePayAuthRequest.getEmailNotice());
        createPaymentRequest.setCurrency(EURO_ISO_CODE);
        createPaymentRequest.setDescription(postePayAuthRequest.getDescription());
        createPaymentRequest.setShopId(configsMap.get(SHOP_ID_CONFIG));

        ResponseURLs responseURLs = new ResponseURLs();
        setResponseUrl(responseURLs, clientId, configsMap.get(RESPONSE_URL_CONFIG));
        createPaymentRequest.setResponseURLs(responseURLs);

        createPaymentRequest.setShopTransactionId(postePayAuthRequest.getIdTransaction().toString());

        return createPaymentRequest;

    }

    private void setResponseUrl(ResponseURLs responseURLs, String clientId, String responseUrl) {

        switch (clientId) {
            case "APP":
                responseURLs.setResponseUrlOk(org.apache.commons.lang3.StringUtils.EMPTY);
                responseURLs.setResponseUrlKo(org.apache.commons.lang3.StringUtils.EMPTY);
                responseURLs.setServerNotificationUrl(POSTEPAY_NOTIFICATION_URL);
                break;
            case "WEB":
                responseURLs.setResponseUrlOk(responseUrl);
                responseURLs.setResponseUrlKo(responseUrl);
                responseURLs.setServerNotificationUrl(POSTEPAY_NOTIFICATION_URL);
                break;
            default:
                break;
        }
    }


    private Map<String, String> getConfigValues(String config) {
        List<String> listConfig = Arrays.asList(config.split("\\|"));

        Map<String, String> configsMap = new HashMap<>();
        configsMap.put(SHOP_ID_CONFIG, listConfig.get(0));
        configsMap.put(PAYMENT_CHANNEL_CONFIG, listConfig.get(1));
        configsMap.put(AUTH_TYPE_CONFIG, listConfig.get(2));
        configsMap.put(RESPONSE_URL_CONFIG, listConfig.size() > 3 ? listConfig.get(3) : "");

        return configsMap;
    }

    private ResponseEntity<PostePayAuthResponse> createPostePayAuthResponse(String channel, String error, boolean isError, HttpStatus status) {

        PostePayAuthResponse postePayAuthResponse = new PostePayAuthResponse();
        postePayAuthResponse.setChannel(channel);
        if (isError) {
            postePayAuthResponse.setError(error);
        } else {
            postePayAuthResponse.setUrlRedirect(PAYMENT_RESPONSE_URLREDIRECT);
        }
        return ResponseEntity.status(status).body(postePayAuthResponse);
    }
}