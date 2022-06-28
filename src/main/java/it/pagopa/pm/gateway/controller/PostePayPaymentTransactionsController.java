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
import org.apache.commons.lang3.StringUtils;
import org.openapitools.client.api.PaymentManagerControllerApi;
import org.openapitools.client.model.InlineResponse200;
import org.openapitools.client.ApiException;
import org.apache.commons.lang3.ObjectUtils;

import org.openapitools.client.model.InlineResponse2001;
import org.openapitools.client.model.InlineResponse2002;
import org.openapitools.client.model.CreatePaymentRequest;
import org.openapitools.client.model.ResponseURLs;
import org.openapitools.client.model.PaymentChannel;
import org.openapitools.client.model.AuthorizationType;
import org.openapitools.client.model.EsitoStorno;
import org.openapitools.client.model.DetailsPaymentRequest;
import org.openapitools.client.model.Esito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_POSTEPAY;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_POSTEPAY_REQUEST_ID;
import static it.pagopa.pm.gateway.constant.ClientConfigs.*;
import static it.pagopa.pm.gateway.constant.ClientConfigs.NOTIFICATION_URL_CONFIG;
import static it.pagopa.pm.gateway.constant.Headers.*;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.LogoPaths.POSTEPAY_LOGO_PATH;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.KO;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.OK;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
public class PostePayPaymentTransactionsController {

    private static final String APP_ORIGIN = "APP";
    private static final String WEB_ORIGIN = "WEB";
    private static final String EURO_ISO_CODE = "978";
    private static final String POSTEPAY_CLIENT_ID_PROPERTY = "postepay.clientId.%s.config";
    private static final String PGS_CLIENT_RESPONSE_URL = "postepay.pgs.response.%s.clientResponseUrl";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> VALID_CLIENT_ID = Arrays.asList(APP_ORIGIN, WEB_ORIGIN);
    private static final String PIPE_SPLIT_CHAR = "\\|";

    @Value("${postepay.pgs.response.urlredirect}")
    private String PGS_RESPONSE_URL_REDIRECT;

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
    private Environment environment;

    @PutMapping(REQUEST_PAYMENTS_POSTEPAY)
    public ACKMessage closePayment(@RequestBody AuthMessage authMessage,
                                   @RequestHeader(X_CORRELATION_ID) String correlationId) throws RestApiException {
        MDC.clear();
        log.info("START - Update PostePay transaction request for correlation-id: " + correlationId + " - authorization: " + authMessage);
        validatePutRequestEntryParams(authMessage, correlationId);
        PaymentRequestEntity requestEntity = paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(correlationId, EndpointEnum.POSTEPAY.getValue());
        validatePutRequestForEntity(correlationId, requestEntity);

        try {
            boolean isAuthOutcomeOk = authMessage.getAuthOutcome() == OK;
            String closePaymentResult = restapiCdClient.callClosePayment(requestEntity.getIdTransaction(), isAuthOutcomeOk, authMessage.getAuthCode());
            log.info("Response from closePayment for correlation-id: " + correlationId + " " + closePaymentResult);
            requestEntity.setIsProcessed(true);
            requestEntity.setAuthorizationOutcome(isAuthOutcomeOk);
            requestEntity.setAuthorizationCode(authMessage.getAuthCode());
            paymentRequestRepository.save(requestEntity);
        } catch (FeignException fe) {
            log.error("Feign exception calling restapi-cd to close payment", fe);
            throw new RestApiException(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR, fe.status());
        } catch (Exception e) {
            log.error("An exception occurred while closing payment", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } finally {
            log.info("END - Update PostePay transaction request for correlation-id: " + correlationId + " - authorization: " + authMessage);
        }
        return new ACKMessage(OK);
    }

    private void validatePutRequestForEntity(String correlationId, PaymentRequestEntity requestEntity) throws RestApiException {
        if (Objects.isNull(requestEntity)) {
            log.error("No PostePay request entity has been found for correlation-id: " + correlationId);
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        } else {
            setMdcFields(requestEntity.getMdcInfo());
            if (Boolean.TRUE.equals(requestEntity.getIsProcessed())) {
                log.error("Transaction associated to correlation-id: " + correlationId + " has already been processed");
                throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
            }
        }
    }

    private void validatePutRequestEntryParams(AuthMessage authMessage, String correlationId) throws RestApiException {
        boolean isAuthOutcomeNotValid = Objects.isNull(authMessage) || authMessage.getAuthOutcome() == null;
        if (isAuthOutcomeNotValid || StringUtils.isBlank(correlationId)) {
            log.error("Invalid request: authorization outcome or correlation-id are blank");
            throw new RestApiException(ExceptionsEnum.MISSING_FIELDS);
        }
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENTS_POSTEPAY)
    public ResponseEntity<PostePayAuthResponse> requestPaymentsPostepay(@RequestHeader(value = X_CLIENT_ID) String clientId,
                                                                        @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                        @RequestBody PostePayAuthRequest postePayAuthRequest) {
        log.info("START - requesting PostePay payment authorization");
        setMdcFields(mdcFields);

        if (ObjectUtils.anyNull(postePayAuthRequest, postePayAuthRequest.getIdTransaction()) || postePayAuthRequest.getGrandTotal() == 0) {
            log.error("Error: mandatory request parameters are missing");
            return createPostePayAuthResponse(clientId, BAD_REQUEST_MSG, HttpStatus.BAD_REQUEST, null);
        }

        if (!VALID_CLIENT_ID.contains(clientId)) {
            log.error("Client id " + clientId + " is not valid");
            return createPostePayAuthResponse(clientId, BAD_REQUEST_MSG_CLIENT_ID, HttpStatus.BAD_REQUEST, null);
        }

        Long idTransaction = postePayAuthRequest.getIdTransaction();
        if (Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createPostePayAuthResponse(clientId, TRANSACTION_ALREADY_PROCESSED_MSG, HttpStatus.UNAUTHORIZED, null);
        }

        log.info(String.format("Requesting authorization from %s channel for transaction %s", clientId, idTransaction));
        PaymentRequestEntity paymentRequestEntity;
        try {
            String authRequestJson = OBJECT_MAPPER.writeValueAsString(postePayAuthRequest);
            log.debug("Resulting postePayAuthRequest JSON string = " + authRequestJson);
            paymentRequestEntity = generateRequestEntity(clientId, mdcFields, idTransaction);
            paymentRequestEntity.setJsonRequest(authRequestJson);
        } catch (JsonProcessingException e) {
            log.error(SERIALIZATION_ERROR_MSG, e);
            return createPostePayAuthResponse(clientId, SERIALIZATION_ERROR_MSG, HttpStatus.INTERNAL_SERVER_ERROR, null);
        }

        try {
            executePostePayAuthorizationCall(postePayAuthRequest, clientId, paymentRequestEntity);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + idTransaction, e);
            return createPostePayAuthResponse(clientId, GENERIC_ERROR_MSG + idTransaction, HttpStatus.INTERNAL_SERVER_ERROR, null);
        }

        log.info("END - POST request-payments/postepay for idTransaction: " + idTransaction);
        return createPostePayAuthResponse(clientId, StringUtils.EMPTY, HttpStatus.OK, paymentRequestEntity.getGuid());
    }

    private PaymentRequestEntity generateRequestEntity(String clientId, String mdcFields, Long idTransaction) {
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_POSTEPAY);
        paymentRequestEntity.setIdTransaction(idTransaction);
        paymentRequestEntity.setMdcInfo(mdcFields);
        return paymentRequestEntity;
    }

    @ResponseBody
    @GetMapping(REQUEST_PAYMENTS_POSTEPAY_REQUEST_ID)
    public PostePayPollingResponse getPostepayAuthorizationResponse(@PathVariable String requestId,
                                                                    @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws RestApiException {
        log.info("START - get PostePay authorization response for GUID: " + requestId);
        setMdcFields(mdcFields);
        PaymentRequestEntity requestEntity = paymentRequestRepository.findByGuid(requestId);
        if (requestEntity == null || !REQUEST_PAYMENTS_POSTEPAY.equals(requestEntity.getRequestEndpoint())) {
            log.error("No PostePay request entity object has been found for GUID " + requestId);
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        }
        return createPollingResponse(requestId, requestEntity);
    }

    @Async
    private void executePostePayAuthorizationCall(PostePayAuthRequest postePayAuthRequest, String clientId, PaymentRequestEntity paymentRequestEntity) throws RestApiException {
        Long idTransaction = postePayAuthRequest.getIdTransaction();
        log.info("START - execute PostePay payment authorization request for transaction " + idTransaction);
        String correlationId;
        String authorizationUrl;
        try {
            CreatePaymentRequest createPaymentRequest = createAuthorizationRequest(postePayAuthRequest, clientId);
            MicrosoftAzureLoginResponse microsoftAzureLoginResponse = azureLoginClient.requestMicrosoftAzureLoginPostepay();
            String bearerToken = BEARER_TOKEN_PREFIX + microsoftAzureLoginResponse.getAccess_token();
            log.debug("bearer token acquired: " + bearerToken);
            InlineResponse200 inlineResponse200 = postePayControllerApi.apiV1PaymentCreatePost(bearerToken, createPaymentRequest);
            if (Objects.isNull(inlineResponse200)) {
                log.error("/createPayment response from PostePay is null");
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            correlationId = inlineResponse200.getPaymentID();
            authorizationUrl = inlineResponse200.getUserRedirectURL();
            log.info(String.format("Response from PostePay /createPayment for idTransaction %s: " +
                    "correlationId = %s - authorizationUrl = %s", idTransaction, correlationId, authorizationUrl));
        } catch (ApiException e) {
            log.error("Error while calling PostePay's /createPayment API. HTTP Status is: " + e.getCode());
            log.error("Response body: " + e.getResponseBody());
            log.error("Complete exception: ", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } catch (Exception e) {
            log.error("An exception occurred while executing PostePay authorization call", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        paymentRequestEntity.setCorrelationId(correlationId);
        paymentRequestEntity.setAuthorizationUrl(authorizationUrl);
        paymentRequestEntity.setResourcePath(POSTEPAY_LOGO_PATH);
        paymentRequestRepository.save(paymentRequestEntity);
        log.info("END - execute PostePay payment authorization request for transaction " + idTransaction);
    }

    private CreatePaymentRequest createAuthorizationRequest(PostePayAuthRequest postePayAuthRequest, String clientId) throws NullPointerException {
        String clientConfig = getCustomEnvironmentProperty(POSTEPAY_CLIENT_ID_PROPERTY, clientId);
        Map<String, String> configsMap = getConfigValues(clientConfig);
        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
        createPaymentRequest.setMerchantId(configsMap.get(MERCHANT_ID_CONFIG));
        createPaymentRequest.setShopId(configsMap.get(SHOP_ID_CONFIG));
        createPaymentRequest.setShopTransactionId(String.valueOf(postePayAuthRequest.getIdTransaction()));
        createPaymentRequest.setAmount(String.valueOf(postePayAuthRequest.getGrandTotal()));
        createPaymentRequest.setDescription(postePayAuthRequest.getDescription());
        createPaymentRequest.setCurrency(EURO_ISO_CODE);
        createPaymentRequest.setBuyerName(postePayAuthRequest.getName());
        createPaymentRequest.setBuyerEmail(postePayAuthRequest.getEmailNotice());
        createPaymentRequest.setPaymentChannel(PaymentChannel.valueOf(configsMap.get(PAYMENT_CHANNEL_CONFIG)));
        createPaymentRequest.setAuthType(AuthorizationType.fromValue(configsMap.get(AUTH_TYPE_CONFIG)));
        ResponseURLs responseURLs = createResponseUrls(clientId, configsMap.get(NOTIFICATION_URL_CONFIG));
        createPaymentRequest.setResponseURLs(responseURLs);
        return createPaymentRequest;

    }

    private ResponseURLs createResponseUrls(String clientId, String responseUrl) {
        ResponseURLs responseURLs = new ResponseURLs();
        switch (clientId) {
            case APP_ORIGIN:
                responseURLs.setResponseUrlOk(StringUtils.EMPTY);
                responseURLs.setResponseUrlKo(StringUtils.EMPTY);
                responseURLs.setServerNotificationUrl(POSTEPAY_NOTIFICATION_URL);
                return responseURLs;
            case WEB_ORIGIN:
                responseURLs.setResponseUrlOk(responseUrl);
                responseURLs.setResponseUrlKo(responseUrl);
                responseURLs.setServerNotificationUrl(POSTEPAY_NOTIFICATION_URL);
                return responseURLs;
            default:
                log.info("ClientId " + clientId + " case is not managed. Returning empty responseUrls");
                return responseURLs;
        }
    }

    private Map<String, String> getConfigValues(String config) {
        List<String> listConfig = Arrays.asList(config.split(PIPE_SPLIT_CHAR));
        Map<String, String> configsMap = new HashMap<>();
        configsMap.put(MERCHANT_ID_CONFIG, listConfig.get(0));
        configsMap.put(SHOP_ID_CONFIG, listConfig.get(1));
        configsMap.put(PAYMENT_CHANNEL_CONFIG, listConfig.get(2));
        configsMap.put(AUTH_TYPE_CONFIG, listConfig.get(3));
        configsMap.put(NOTIFICATION_URL_CONFIG, listConfig.size() > 4 ? listConfig.get(4) : StringUtils.EMPTY);
        return configsMap;
    }

    private ResponseEntity<PostePayAuthResponse> createPostePayAuthResponse(String channel, String errorMessage, HttpStatus status, String requestId) {
        PostePayAuthResponse postePayAuthResponse = new PostePayAuthResponse();
        postePayAuthResponse.setChannel(channel);
        if (StringUtils.isEmpty(errorMessage)) {
            String urlRedirect = StringUtils.join(PGS_RESPONSE_URL_REDIRECT, requestId);
            postePayAuthResponse.setUrlRedirect(urlRedirect);
        } else {
            postePayAuthResponse.setError(errorMessage);
            log.info("END - execute PostePay payment authorization");
        }
        return ResponseEntity.status(status).body(postePayAuthResponse);
    }

    private PostePayPollingResponse createPollingResponse(String requestId, PaymentRequestEntity entity) {
        PostePayPollingResponse response = new PostePayPollingResponse();
        Boolean outcome = entity.getAuthorizationOutcome();
        OutcomeEnum authorizationOutcome = Objects.isNull(outcome) ? null : outcome ? OK : KO;
        String urlRedirect = entity.getAuthorizationUrl();
        response.setUrlRedirect(urlRedirect);
        response.setAuthOutcome(authorizationOutcome);
        response.setChannel(entity.getClientId());
        if (Objects.isNull(authorizationOutcome)) {
            log.warn("No authorization outcome has been received yet for requestId " + requestId);
            response.setError("No authorization outcome has been received yet");
        } else if (authorizationOutcome.equals(KO)) {
            log.error("Authorization is KO for requestId " + requestId);
            response.setError("Payment authorization has not been granted");
        } else {
            String clientResponseUrl = getCustomEnvironmentProperty(PGS_CLIENT_RESPONSE_URL, entity.getClientId());
            response.setClientResponseUrl(clientResponseUrl);
            response.setLogoResourcePath(entity.getResourcePath());
            response.setError(StringUtils.EMPTY);
        }
        log.info("END - get PostePay authorization response for GUID: " + requestId + " - authorization is " + authorizationOutcome);
        return response;
    }

    private ResponseEntity<PostePayRefundResponse> createPostePayRefundResponse(String requestId, String paymentId, String refundOutcome,
                                                                                ExceptionsEnum exceptionsEnum) {
        PostePayRefundResponse postePayRefundResponse = new PostePayRefundResponse();
        postePayRefundResponse.setRequestId(requestId);
        postePayRefundResponse.setPaymentId(paymentId);
        postePayRefundResponse.setRefundOutcome(refundOutcome);
        if (Objects.nonNull(exceptionsEnum)) {
            postePayRefundResponse.setError(exceptionsEnum.getDescription());
            return ResponseEntity.status(exceptionsEnum.getRestApiCode()).body(postePayRefundResponse);
        }
        return ResponseEntity.status(HttpStatus.OK).body(postePayRefundResponse);

    }

    private String getCustomEnvironmentProperty(String parameterizedPropertyName, String clientId) throws NullPointerException {
        String propertyToSearch = String.format(parameterizedPropertyName, clientId);
        if (StringUtils.isNotBlank(propertyToSearch)) {
            String property = environment.getProperty(propertyToSearch);
            if (StringUtils.isNotBlank(property)) {
                return property;
            } else {
                log.error("Environment property " + propertyToSearch + " is blank or does not exist");
                throw new NullPointerException();
            }
        } else {
            log.error("Environment property to search is blank");
            throw new NullPointerException();
        }

    }

    @DeleteMapping(REQUEST_PAYMENTS_POSTEPAY_REQUEST_ID)
    public ResponseEntity<PostePayRefundResponse> refundPostePayPayment(@PathVariable String requestId,
                                                                        @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws RestApiException {
        setMdcFields(mdcFields);
        log.info("START - refund PostePay request for paymentRequestEntity with guid: " + requestId);

        PaymentRequestEntity requestEntity = paymentRequestRepository.findByGuid(requestId);

        if (Objects.isNull(requestEntity) || !REQUEST_PAYMENTS_POSTEPAY.equals(requestEntity.getRequestEndpoint())) {
            log.error("No PostePay response entity object has been found with guid " + requestId);
            return createPostePayRefundResponse(requestId, null, null, ExceptionsEnum.TRANSACTION_NOT_FOUND);
        }

        if (requestEntity.getIsRefund()) {
            log.info("Transaction with id " + requestEntity.getIdTransaction() + " has already be refunded");
            return createPostePayRefundResponse(requestId, requestEntity.getCorrelationId(), null, ExceptionsEnum.TRANSACTION_ALREADY_REFUNDED);
        }

        // step 1. acquiring payment details PostePay side in order to validate the deletion request
        DetailsPaymentRequest detailsPaymentRequest = createDetailPaymentRequest(requestEntity);

        boolean isAuthCodePresent = StringUtils.isEmpty(requestEntity.getAuthorizationCode());
        boolean checkDetail = true;

        if (isAuthCodePresent) {
            try {
                log.info("Starting check Details for detailsPaymentRequest with guid: " + requestEntity.getGuid());
                checkDetail = checkDetailStatus(detailsPaymentRequest);
            } catch (Exception e) {
                log.error("Generic Exception while checkDetailStatus DetailsPaymentRequest with paymentId: "
                        + detailsPaymentRequest.getPaymentID() + " error message " + e.getMessage());
            }
        }

        if (isAuthCodePresent || checkDetail) {
            return executeRefundRequest(detailsPaymentRequest, requestEntity);
        }

        return createPostePayRefundResponse(requestId, requestEntity.getCorrelationId(), null, ExceptionsEnum.TRANSACTION_REFUND_NOT_AUTHORIZED);

    }

    private ResponseEntity<PostePayRefundResponse> executeRefundRequest(DetailsPaymentRequest detailsPaymentRequest, PaymentRequestEntity requestEntity) {
        InlineResponse2002 inlineResponse2002;
        log.info("START - execute refund PostePay request for paymentRequestEntity with guid: " + requestEntity.getGuid());

        try {
            inlineResponse2002 = postePayControllerApi.apiV1PaymentRefundPost(detailsPaymentRequest);
            if (Objects.isNull(inlineResponse2002)) {
                log.error("Response for request refund for paymentId: " + detailsPaymentRequest.getPaymentID() + " is null");
                return createPostePayRefundResponse(requestEntity.getGuid(), requestEntity.getCorrelationId(), null, ExceptionsEnum.PSP_CLIENT_EXCEPTION);
            }
            requestEntity.setIsRefund(inlineResponse2002.getTransactionResult().equals(EsitoStorno.OK));
            paymentRequestRepository.save(requestEntity);
            return createPostePayRefundResponse(requestEntity.getGuid(), requestEntity.getCorrelationId(), inlineResponse2002.getTransactionResult().getValue(), null);

        } catch (ApiException e) {
            log.error("Exception while calling Postepay PSP for refund DetailsPaymentRequest with paymentId: " + detailsPaymentRequest.getPaymentID());

            if (e.getCode() == HttpStatus.REQUEST_TIMEOUT.value()) {
                return createPostePayRefundResponse(requestEntity.getGuid(), requestEntity.getCorrelationId(), null, ExceptionsEnum.TIMEOUT);
            }
            return createPostePayRefundResponse(requestEntity.getGuid(), requestEntity.getCorrelationId(), null, ExceptionsEnum.PSP_CLIENT_EXCEPTION);
        } catch (Exception e) {
            log.error("Generic Exception while refund DetailsPaymentRequest with paymentId: " + detailsPaymentRequest.getPaymentID());
            return createPostePayRefundResponse(requestEntity.getGuid(), requestEntity.getCorrelationId(), null, ExceptionsEnum.GENERIC_ERROR);

        }

    }

    private boolean checkDetailStatus(DetailsPaymentRequest detailsPaymentRequest) throws Exception {
        InlineResponse2001 inlineResponse2001;
        inlineResponse2001 = postePayControllerApi.apiV1PaymentDetailsPost(detailsPaymentRequest);
        if (Objects.isNull(inlineResponse2001)) {
            throw new RestApiException(ExceptionsEnum.PSP_CLIENT_EXCEPTION);
        }

        Esito esito = inlineResponse2001.getStatus();
        return !Objects.isNull(esito) && esito.equals(Esito.APPROVED);

    }


    private DetailsPaymentRequest createDetailPaymentRequest(PaymentRequestEntity paymentRequestEntity) {
        String clientConfig = getCustomEnvironmentProperty(POSTEPAY_CLIENT_ID_PROPERTY, paymentRequestEntity.getClientId());
        String shopId = getConfigValues(clientConfig).get(SHOP_ID_CONFIG);
        DetailsPaymentRequest detailsPaymentRequest = new DetailsPaymentRequest();
        detailsPaymentRequest.setPaymentID(paymentRequestEntity.getCorrelationId());
        detailsPaymentRequest.setShopId(shopId);
        detailsPaymentRequest.setShopTransactionId(String.valueOf(paymentRequestEntity.getIdTransaction()));
        return detailsPaymentRequest;

    }

}