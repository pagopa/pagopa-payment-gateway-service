package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.pagopa.pm.gateway.client.azure.AzureLoginClient;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.constant.Scopes;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.enums.EndpointEnum;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.StatusErrorCodeOutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.PaymentManagerControllerApi;
import org.openapitools.client.api.UserApi;
import org.openapitools.client.model.*;
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

import static it.pagopa.pm.gateway.constant.ApiPaths.POSTEPAY_REQUEST_PAYMENTS_PATH;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_POSTEPAY;
import static it.pagopa.pm.gateway.constant.ClientConfigs.*;
import static it.pagopa.pm.gateway.constant.Headers.*;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.constant.Params.IS_ONBOARDING_PARAM;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.KO;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.OK;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
public class PostePayPaymentTransactionsController {

    private static final String APP_ORIGIN = "APP";
    private static final String WEB_ORIGIN = "WEB";
    private static final String EURO_ISO_CODE = "978";
    private static final String POSTEPAY_CLIENT_ID_PROPERTY = "postepay.clientId.%s.config";
    private static final String PGS_CLIENT_RESPONSE_URL = "postepay.pgs.response.%s.clientResponseUrl.payment";
    private static final String PGS_CLIENT_RESPONSE_URL_ONBOARDING = "postepay.pgs.response.clientResponseUrl.onboarding";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> VALID_CLIENT_ID = Arrays.asList(APP_ORIGIN, WEB_ORIGIN);
    private static final String PIPE_SPLIT_CHAR = "\\|";
    private static final String RESPONSE_BODY_MSG = "Response body: ";
    private static final String COMPLETE_EXCEPTION_MSG = "Complete exception: ";

    @Value("${pgs.postepay.response.urlredirect}")
    private String pgsResponseUrlRedirect;

    @Value("${pgs.postepay.notificationURL}")
    private String postepayNotificationUrl;

    @Value("${pgs.postepay.logo.url}")
    private String postepayLogoUrl;

    @Autowired
    private AzureLoginClient azureLoginClient;

    @Autowired
    private RestapiCdClientImpl restapiCdClient;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private PaymentManagerControllerApi postePayControllerApi;

    @Autowired
    private UserApi userApi;

    @Autowired
    private Environment environment;

    @PutMapping(REQUEST_PAYMENTS_POSTEPAY)
    public ResponseEntity<ACKMessage> updatePostePayTransaction(@RequestBody AuthMessage authMessage,
                                                                @RequestHeader(X_CORRELATION_ID) String correlationId) throws RestApiException {
        MDC.clear();
        log.info("START - Update PostePay transaction request for correlation-id: " + correlationId + " - authorization: " + authMessage);
        validatePutRequestEntryParams(authMessage, correlationId);
        PaymentRequestEntity requestEntity = paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(correlationId, EndpointEnum.POSTEPAY.getValue());
        validatePutRequestForEntity(correlationId, requestEntity);

        try {
            boolean isAuthOutcomeOk = authMessage.getAuthOutcome() == OK;

            String authCode = authMessage.getAuthCode();
            if (requestEntity.getIsOnboarding()) {
                log.info("This is an onboarding payment: skipping call to PATCH API on PM");
            } else {
                Long transactionStatus = isAuthOutcomeOk ? TransactionStatusEnum.TX_AUTHORIZED_BY_PGS.getId() : TransactionStatusEnum.TX_REFUSED.getId();
                PostePayPatchRequest postePayPatchRequest = new PostePayPatchRequest(transactionStatus, authCode, correlationId);
                String closePaymentResult = restapiCdClient.callUpdatePostePayTransaction(Long.valueOf(requestEntity.getIdTransaction()), postePayPatchRequest);
                log.info("Response from PATCH updateTransaction for correlation-id: " + correlationId + " " + closePaymentResult);
            }
            requestEntity.setIsProcessed(true);
            requestEntity.setAuthorizationOutcome(isAuthOutcomeOk);
            requestEntity.setAuthorizationCode(authCode);
            requestEntity.setStatus(isAuthOutcomeOk ? AUTHORIZED.name() : DENIED.name());
            paymentRequestRepository.save(requestEntity);
        } catch (FeignException fe) {
            log.error("A feign exception occurred while calling restapi-cd updateTransaction PATCH API", fe);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ACKMessage(OutcomeEnum.KO));
        } catch (Exception e) {
            log.error("An exception occurred while calling restapi-cd updateTransaction PATCH API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ACKMessage(OutcomeEnum.KO));
        } finally {
            log.info("END - Update PostePay transaction request for correlation-id: " + correlationId + " - authorization: " + authMessage);
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ACKMessage(OutcomeEnum.OK));
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
                                                                        @RequestBody PostePayAuthRequest postePayAuthRequest,
                                                                        @RequestParam(required = false, value = IS_ONBOARDING_PARAM) Boolean isOnboarding) {
        log.info("START - " + REQUEST_PAYMENTS_POSTEPAY);
        setMdcFields(mdcFields);

        if (ObjectUtils.anyNull(postePayAuthRequest, postePayAuthRequest.getIdTransaction()) || postePayAuthRequest.getGrandTotal() == 0) {
            log.error("Error: mandatory request parameters are missing");
            return createPostePayAuthResponse(clientId, BAD_REQUEST_MSG, HttpStatus.BAD_REQUEST, null, null);
        }

        if (!VALID_CLIENT_ID.contains(clientId)) {
            log.error("Client id " + clientId + " is not valid");
            return createPostePayAuthResponse(clientId, BAD_REQUEST_MSG_CLIENT_ID, HttpStatus.BAD_REQUEST, null, null);
        }

        String idTransaction = postePayAuthRequest.getIdTransaction();
        if (Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createPostePayAuthResponse(clientId, TRANSACTION_ALREADY_PROCESSED_MSG, HttpStatus.UNAUTHORIZED, null, null);
        }

        ResponseEntity<PostePayAuthResponse> response = BooleanUtils.toBoolean(isOnboarding) ?
                createOnboardingPostePay(clientId, mdcFields, new PostePayOnboardingRequest(idTransaction)) :
                createPaymentPostePay(clientId, mdcFields, postePayAuthRequest);
        log.info(String.format("END - %s for idTransaction %s", REQUEST_PAYMENTS_POSTEPAY, idTransaction));
        return response;
    }

    private PaymentRequestEntity generateRequestEntity(String clientId, String mdcFields, String idTransaction, boolean isOnboarding) {
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_POSTEPAY);
        paymentRequestEntity.setIdTransaction(idTransaction);
        paymentRequestEntity.setMdcInfo(mdcFields);
        paymentRequestEntity.setIsOnboarding(BooleanUtils.toBoolean(isOnboarding));
        paymentRequestEntity.setStatus(CREATED.name());
        return paymentRequestEntity;
    }

    private ResponseEntity<PostePayAuthResponse> createPaymentPostePay(String clientId, String mdcFields, PostePayAuthRequest postePayAuthRequest) {
        log.info("START - PostePay Request Payment");
        String idTransaction = postePayAuthRequest.getIdTransaction();

        log.info(String.format("Requesting authorization from %s channel for transaction %s", clientId, idTransaction));
        PaymentRequestEntity paymentRequestEntity;
        try {
            String authRequestJson = OBJECT_MAPPER.writeValueAsString(postePayAuthRequest);
            log.debug("Resulting postePayAuthRequest JSON string = " + authRequestJson);
            paymentRequestEntity = generateRequestEntity(clientId, mdcFields, idTransaction, false);
            paymentRequestEntity.setJsonRequest(authRequestJson);
        } catch (JsonProcessingException e) {
            log.error(SERIALIZATION_ERROR_MSG, e);
            return createPostePayAuthResponse(clientId, SERIALIZATION_ERROR_MSG, HttpStatus.INTERNAL_SERVER_ERROR, null, null);
        }

        try {
            executePostePayAuthorizationCall(postePayAuthRequest, clientId, paymentRequestEntity);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + idTransaction, e);
            return createPostePayAuthResponse(clientId, GENERIC_ERROR_MSG + idTransaction, HttpStatus.INTERNAL_SERVER_ERROR, null, null);
        }

        log.info("END - PostePay Request Payment ");
        return createPostePayAuthResponse(clientId, StringUtils.EMPTY, HttpStatus.OK, paymentRequestEntity.getGuid(), paymentRequestEntity.getCorrelationId());
    }

    private ResponseEntity<PostePayAuthResponse> createOnboardingPostePay(String clientId, String mdcFields, PostePayOnboardingRequest postePayOnboardingRequest) {
        log.info("START - PostePay Onboarding");
        String onboardingTransactionId = postePayOnboardingRequest.getOnboardingTransactionId();
        log.info(String.format("Requesting authorization from %s channel for transaction %s", clientId, onboardingTransactionId));
        PaymentRequestEntity paymentRequestEntity;
        try {
            String authRequestJson = OBJECT_MAPPER.writeValueAsString(postePayOnboardingRequest);
            log.debug("Resulting postePayAuthRequest JSON string = " + authRequestJson);
            paymentRequestEntity = generateRequestEntity(clientId, mdcFields, onboardingTransactionId, true);
            paymentRequestEntity.setJsonRequest(authRequestJson);
        } catch (JsonProcessingException e) {
            log.error(SERIALIZATION_ERROR_MSG, e);
            return createPostePayAuthResponse(clientId, SERIALIZATION_ERROR_MSG, HttpStatus.INTERNAL_SERVER_ERROR, null, null);
        }

        try {
            executePostePayOnboardingCall(postePayOnboardingRequest, clientId, paymentRequestEntity);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + onboardingTransactionId, e);
            return createPostePayAuthResponse(clientId, GENERIC_ERROR_MSG + onboardingTransactionId, HttpStatus.INTERNAL_SERVER_ERROR, null, null);
        }

        log.info("END - PostePay Onboarding");
        return createPostePayAuthResponse(clientId, StringUtils.EMPTY, HttpStatus.OK, paymentRequestEntity.getGuid(), paymentRequestEntity.getCorrelationId());
    }

    @ResponseBody
    @GetMapping(POSTEPAY_REQUEST_PAYMENTS_PATH)
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

    private String acquireBearerToken() throws RestApiException {
        try {
            MicrosoftAzureLoginResponse microsoftAzureLoginResponse = azureLoginClient.requestMicrosoftAzureLoginPostepay();
            log.info("bearer token acquired");
            return BEARER_TOKEN_PREFIX + microsoftAzureLoginResponse.getAccess_token();
        } catch (Exception e) {
            log.error("An exception occurred while acquiring the bearer token", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
    }

    @Async
    private void executePostePayAuthorizationCall(PostePayAuthRequest postePayAuthRequest, String clientId, PaymentRequestEntity paymentRequestEntity) throws RestApiException {
        String idTransaction = postePayAuthRequest.getIdTransaction();
        log.info("START - execute PostePay payment authorization request for transaction " + idTransaction);
        String correlationId;
        String authorizationUrl;
        try {
            CreatePaymentRequest createPaymentRequest = createPaymentAuthorizationRequest(postePayAuthRequest, clientId);
            String bearerToken = acquireBearerToken();
            CreatePaymentResponse createPaymentResponse = postePayControllerApi.apiV1PaymentCreatePost(bearerToken, createPaymentRequest);
            if (Objects.isNull(createPaymentResponse)) {
                log.error("/createPayment response from PostePay is null");
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            correlationId = createPaymentResponse.getPaymentID();
            authorizationUrl = createPaymentResponse.getUserRedirectURL();
            log.info(String.format("Response from PostePay /createPayment for idTransaction %s: " +
                    "correlationId = %s - authorizationUrl = %s", idTransaction, correlationId, authorizationUrl));
        } catch (ApiException e) {
            log.error("Error while calling PostePay's /createPayment API. HTTP Status is: " + e.getCode());
            logException(e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } catch (Exception e) {
            log.error("An exception occurred while executing PostePay authorization call", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        paymentRequestEntity.setCorrelationId(correlationId);
        paymentRequestEntity.setAuthorizationUrl(authorizationUrl);
        paymentRequestEntity.setResourcePath(postepayLogoUrl);
        paymentRequestRepository.save(paymentRequestEntity);
        log.info("END - execute PostePay payment authorization request for transaction " + idTransaction);
    }

    @Async
    private void executePostePayOnboardingCall(PostePayOnboardingRequest postePayOnboardingRequest, String clientId, PaymentRequestEntity paymentRequestEntity) throws RestApiException {
        String onboardingTransactionId = postePayOnboardingRequest.getOnboardingTransactionId();
        log.info("START - execute PostePay onboarding for transaction " + onboardingTransactionId);
        String correlationId;
        String authorizationUrl;
        try {
            OnboardingRequest onboardingRequest = createOnboardingRequest(postePayOnboardingRequest, clientId);
            String bearerToken = acquireBearerToken();
            OnboardingResponse onboardingResponse = userApi.apiV1UserOnboardingPost(bearerToken, onboardingRequest);

            if (Objects.isNull(onboardingResponse)) {
                log.error("/onboarding response from PostePay is null");
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            correlationId = onboardingResponse.getOnboardingID();
            authorizationUrl = onboardingResponse.getUserRedirectURL();
            log.info(String.format("Response from PostePay /onboarding for idTransaction %s: " +
                    "correlationId = %s - authorizationUrl = %s", onboardingTransactionId, correlationId, authorizationUrl));
        } catch (ApiException e) {
            log.error("Error while calling PostePay's /onboarding API. HTTP Status is: " + e.getCode());
            logException(e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } catch (Exception e) {
            log.error("An exception occurred while executing PostePay onboarding authorization call", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        paymentRequestEntity.setCorrelationId(correlationId);
        paymentRequestEntity.setAuthorizationUrl(authorizationUrl);
        paymentRequestEntity.setResourcePath(postepayLogoUrl);
        paymentRequestRepository.save(paymentRequestEntity);
        log.info("END - execute PostePay onboarding authorization request for transaction " + onboardingTransactionId);
    }

    private CreatePaymentRequest createPaymentAuthorizationRequest(PostePayAuthRequest postePayAuthRequest, String clientId) throws NullPointerException {

        String clientConfig = getCustomEnvironmentProperty(POSTEPAY_CLIENT_ID_PROPERTY, clientId);
        Map<String, String> configsMap = getConfigValues(clientConfig);
        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
        createPaymentRequest.setMerchantId(configsMap.get(MERCHANT_ID_CONFIG));
        createPaymentRequest.setShopId(configsMap.get(SHOP_ID_CONFIG));
        createPaymentRequest.setShopTransactionId(postePayAuthRequest.getIdTransaction());
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

    private OnboardingRequest createOnboardingRequest(PostePayOnboardingRequest postePayOnboardingRequest, String clientId) {
        String clientConfig = getCustomEnvironmentProperty(POSTEPAY_CLIENT_ID_PROPERTY, clientId);
        Map<String, String> configsMap = getConfigValues(clientConfig);
        OnboardingRequest onboardingRequest = new OnboardingRequest();
        onboardingRequest.setMerchantId(configsMap.get(MERCHANT_ID_CONFIG));
        onboardingRequest.setShopId(configsMap.get(SHOP_ID_CONFIG));
        onboardingRequest.setOnboardingTransactionId(postePayOnboardingRequest.getOnboardingTransactionId());
        onboardingRequest.setPaymentChannel(PaymentChannel.valueOf(configsMap.get(PAYMENT_CHANNEL_CONFIG)));
        ResponseURLs responseURLs = createResponseUrls(clientId, configsMap.get(NOTIFICATION_URL_CONFIG));
        onboardingRequest.setResponseURLs(responseURLs);
        return onboardingRequest;
    }

    private ResponseURLs createResponseUrls(String clientId, String responseUrl) {
        ResponseURLs responseURLs = new ResponseURLs();
        switch (clientId) {
            case APP_ORIGIN:
                responseURLs.setResponseUrlOk(StringUtils.EMPTY);
                responseURLs.setResponseUrlKo(StringUtils.EMPTY);
                responseURLs.setServerNotificationUrl(postepayNotificationUrl);
                return responseURLs;
            case WEB_ORIGIN:
                responseURLs.setResponseUrlOk(responseUrl);
                responseURLs.setResponseUrlKo(responseUrl);
                responseURLs.setServerNotificationUrl(postepayNotificationUrl);
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

    private ResponseEntity<PostePayAuthResponse> createPostePayAuthResponse(String channel, String errorMessage, HttpStatus status, String requestId, String correlationId) {
        PostePayAuthResponse postePayAuthResponse = new PostePayAuthResponse();
        postePayAuthResponse.setChannel(channel);
        postePayAuthResponse.setRequestId(requestId);
        postePayAuthResponse.setCorrelationId(correlationId);
        if (StringUtils.isEmpty(errorMessage)) {
            String urlRedirect = String.format(pgsResponseUrlRedirect, requestId, Scopes.POSTEPAY_SCOPE);
            postePayAuthResponse.setUrlRedirect(urlRedirect);
        } else {
            postePayAuthResponse.setError(errorMessage);
            log.info("END - execute PostePay payment authorization");
        }
        return ResponseEntity.status(status).body(postePayAuthResponse);
    }

    private PostePayPollingResponse createPollingResponse(String requestId, PaymentRequestEntity entity) {
        PostePayPollingResponse response = new PostePayPollingResponse();
        String guid = entity.getGuid();
        String status = entity.getStatus();
        Boolean outcome = entity.getAuthorizationOutcome();
        String urlRedirect = entity.getAuthorizationUrl();
        OutcomeEnum authorizationOutcome;
        if (Objects.isNull(outcome)) {
            authorizationOutcome = null;
        } else {
            authorizationOutcome = BooleanUtils.isTrue(outcome) ? OK :KO;
        }
        response.setUrlRedirect(urlRedirect);
        response.setAuthOutcome(authorizationOutcome);
        response.setChannel(entity.getClientId());
        response.setRequestId(guid);
        response.setCorrelationId(entity.getCorrelationId());
        response.setIsOnboarding(entity.getIsOnboarding());
        if (ObjectUtils.isNotEmpty(status)) {
            try {
                response.setPaymentRequestStatus(getEnumValueFromString(status));
            } catch (IllegalArgumentException iae) {
                String errorMsg = String.format("Invalid status %s for request entity with GUID: %s", status, guid);
                log.error(errorMsg);
                response.setError(errorMsg);
            }
        }
        if (Objects.isNull(authorizationOutcome)) {
            log.warn("No authorization outcome has been received yet for requestId " + requestId);
            response.setError("No authorization outcome has been received yet");
        } else if (authorizationOutcome.equals(KO)) {
            log.error("Authorization is KO for requestId " + requestId);
            response.setError("Payment authorization has not been granted");
            response.setStatusErrorCodeOutcome(StatusErrorCodeOutcomeEnum.getEnum(ExceptionsEnum.GENERIC_ERROR));
        } else {
            String clientResponseUrl = BooleanUtils.isTrue(entity.getIsOnboarding()) ?
                    environment.getProperty(PGS_CLIENT_RESPONSE_URL_ONBOARDING) :
                    String.format(getCustomEnvironmentProperty(PGS_CLIENT_RESPONSE_URL, entity.getClientId()), requestId);
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
            String errorDescription = exceptionsEnum.getDescription();
            log.warn("Transaction has not been refunded. Reason: " + errorDescription);
            postePayRefundResponse.setError(errorDescription);
            return ResponseEntity.status(exceptionsEnum.getRestApiCode()).body(postePayRefundResponse);
        }
        log.info("END - PostePay refund for requestId " + requestId);
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

    @DeleteMapping(POSTEPAY_REQUEST_PAYMENTS_PATH)
    public ResponseEntity<PostePayRefundResponse> refundPostePayPayment(@PathVariable String requestId,
                                                                        @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) {
        setMdcFields(mdcFields);
        log.info("START - requesting PostePay refund for requestId: " + requestId);

        PaymentRequestEntity requestEntity = paymentRequestRepository.findByGuid(requestId);

        if (Objects.isNull(requestEntity) || !REQUEST_PAYMENTS_POSTEPAY.equals(requestEntity.getRequestEndpoint())) {
            log.error("No PostePay request entity object has been found with guid " + requestId);
            return createPostePayRefundResponse(requestId, null, null, ExceptionsEnum.PAYMENT_REQUEST_NOT_FOUND);
        }

        String correlationId = requestEntity.getCorrelationId();
        boolean isAuthorizationApproved = StringUtils.isNotEmpty(requestEntity.getAuthorizationCode());

        if (requestEntity.getIsRefunded()) {
            log.info("RequestId " + requestId + " has been refunded already. Skipping refund");
            return createPostePayRefundResponse(requestId, correlationId, null, ExceptionsEnum.REFUND_REQUEST_ALREADY_PROCESSED);
        }

        String bearerToken;
        try {
            bearerToken = acquireBearerToken();
        } catch (RestApiException e) {
            log.error("Error while acquiring the bearer token");
            return createPostePayRefundResponse(requestId, correlationId, null, ExceptionsEnum.POSTEPAY_SERVICE_EXCEPTION);
        }

        if (!isAuthorizationApproved) {
            try {
                log.info(String.format("An authorization code for request %s has not been acquired yet. " +
                        "Calling PostePay details API to acquire authorization status", requestId));
                DetailsPaymentRequest detailsPaymentRequest = createDetailPaymentRequest(requestEntity);
                isAuthorizationApproved = checkDetailStatus(bearerToken, detailsPaymentRequest);
            } catch (Exception e) {
                log.warn("An exception occurred while checking the authorization status for request id " + requestId);
                log.warn("Proceeding anyway with refund request");
                isAuthorizationApproved = true;
            }
        }

        if (!isAuthorizationApproved) {
            return createPostePayRefundResponse(requestId, correlationId, null, ExceptionsEnum.REFUND_NOT_AUTHORIZED);
        } else {
            RefundPaymentRequest refundPaymentRequest = createRefundRequest(requestEntity);
            return executeRefundRequest(bearerToken, refundPaymentRequest, requestEntity);
        }
    }

    private ResponseEntity<PostePayRefundResponse> executeRefundRequest(String bearerToken, RefundPaymentRequest refundPaymentRequest, PaymentRequestEntity requestEntity) {
        String requestId = requestEntity.getGuid();
        String correlationId = requestEntity.getCorrelationId();
        log.info("START - execute PostePay refund for request id: " + requestId);

        RefundPaymentResponse response;
        EsitoStorno refundOutcome;

        try {
            response = postePayControllerApi.apiV1PaymentRefundPost(bearerToken, refundPaymentRequest);

            if (ObjectUtils.isEmpty(response)) {
                log.error("Response to PostePay /refund API is null or empty");
                return createPostePayRefundResponse(requestId, correlationId, null, ExceptionsEnum.POSTEPAY_SERVICE_EXCEPTION);
            } else {
                refundOutcome = response.getTransactionResult();
                log.info(String.format("Refund outcome for request id %s is: %s", requestId, refundOutcome));
                if (ObjectUtils.isEmpty(refundOutcome)) {
                    return createPostePayRefundResponse(requestId, correlationId, null, ExceptionsEnum.POSTEPAY_SERVICE_EXCEPTION);
                }
            }

            boolean isRefunded = refundOutcome.equals(EsitoStorno.OK);
            if (isRefunded) {
                requestEntity.setStatus(CANCELLED.name());
            }
            requestEntity.setIsRefunded(isRefunded);
            paymentRequestRepository.save(requestEntity);
            return createPostePayRefundResponse(requestId, correlationId, refundOutcome.getValue(), null);
        } catch (ApiException e) {
            log.error("Error while calling PostePay's /refund API. HTTP Status is: " + e.getCode());
            log.error(RESPONSE_BODY_MSG + e.getResponseBody());
            log.error(COMPLETE_EXCEPTION_MSG, e);
            return createPostePayRefundResponse(requestId, correlationId, null, ExceptionsEnum.POSTEPAY_SERVICE_EXCEPTION);
        } catch (Exception e) {
            log.error("An exception occurred while requesting PostePay payment refund", e);
            return createPostePayRefundResponse(requestId, correlationId, null, ExceptionsEnum.GENERIC_ERROR);
        }
    }

    private boolean checkDetailStatus(String bearerToken, DetailsPaymentRequest detailsPaymentRequest) throws Exception {
        String paymentId = detailsPaymentRequest.getPaymentID();
        log.info("START - check details for Payment Request with payment id: " + paymentId);
        DetailsPaymentResponse response;
        try {
            response = postePayControllerApi.apiV1PaymentDetailsPost(bearerToken, detailsPaymentRequest);
        } catch (ApiException e) {
            log.error("Error while calling PostePay's /details API. HTTP Status is: " + e.getCode());
            log.error(RESPONSE_BODY_MSG + e.getResponseBody());
            log.error(COMPLETE_EXCEPTION_MSG, e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } catch (Exception e) {
            log.error("An exception occurred while requesting PostePay payment details", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }

        if (Objects.isNull(response)) {
            log.error("Call to PostePay /details API returned an empty response");
            throw new RestApiException(ExceptionsEnum.EMPTY_RESPONSE);
        }

        Esito esito = response.getStatus();
        if (Objects.nonNull(esito)) {
            log.info(String.format("END - check details for payment request with payment id: %s " +
                    "- esito: %s", paymentId, esito.getValue()));
            return esito.equals(Esito.APPROVED);
        } else {
            log.info(String.format("END - check Details for payment request with payment id: %s - esito is null " +
                    "- proceeding with refund anyway...", paymentId));
            return true;
        }
    }

    private DetailsPaymentRequest createDetailPaymentRequest(PaymentRequestEntity paymentRequestEntity) {
        String clientConfig = getCustomEnvironmentProperty(POSTEPAY_CLIENT_ID_PROPERTY, paymentRequestEntity.getClientId());
        String shopId = getConfigValues(clientConfig).get(SHOP_ID_CONFIG);
        DetailsPaymentRequest detailsPaymentRequest = new DetailsPaymentRequest();
        detailsPaymentRequest.setPaymentID(paymentRequestEntity.getCorrelationId());
        detailsPaymentRequest.setShopId(shopId);
        detailsPaymentRequest.setShopTransactionId(paymentRequestEntity.getIdTransaction());
        return detailsPaymentRequest;

    }

    private RefundPaymentRequest createRefundRequest(PaymentRequestEntity requestEntity) {
        String clientConfig = getCustomEnvironmentProperty(POSTEPAY_CLIENT_ID_PROPERTY, requestEntity.getClientId());
        Map<String, String> configValues = getConfigValues(clientConfig);
        RefundPaymentRequest refundPaymentRequest = new RefundPaymentRequest();
        refundPaymentRequest.setMerchantId(configValues.get(MERCHANT_ID_CONFIG));
        refundPaymentRequest.setShopId(configValues.get(SHOP_ID_CONFIG));
        refundPaymentRequest.setShopTransactionId(requestEntity.getIdTransaction());
        refundPaymentRequest.setCurrency(EURO_ISO_CODE);
        refundPaymentRequest.setPaymentID(requestEntity.getCorrelationId());
        refundPaymentRequest.setAuthNumber(requestEntity.getAuthorizationCode());
        return refundPaymentRequest;
    }

    private void logException(ApiException e) {
        log.error(RESPONSE_BODY_MSG + e.getResponseBody());
        log.error(COMPLETE_EXCEPTION_MSG, e);
    }

}