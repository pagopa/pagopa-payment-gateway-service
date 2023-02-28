package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.enums.RefundOutcome;
import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import it.pagopa.pm.gateway.dto.transaction.TransactionInfo;
import it.pagopa.pm.gateway.dto.transaction.UpdateAuthRequest;
import it.pagopa.pm.gateway.dto.vpos.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import it.pagopa.pm.gateway.utils.JwtTokenUtils;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.constant.VposConstant.*;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;
import static it.pagopa.pm.gateway.dto.enums.RefundOutcome.KO;
import static it.pagopa.pm.gateway.dto.enums.RefundOutcome.OK;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@Service
@Slf4j
@NoArgsConstructor
public class VposService {

    private static final String CAUSE = " cause: ";
    private static final List<String> METHOD_CHALLENGE_CODES = Arrays.asList("25", "26");
    private String vposUrl;
    private String vposPollingUrl;
    private PaymentRequestRepository paymentRequestRepository;
    private EcommerceClient ecommerceClient;
    private VPosRequestUtils vPosRequestUtils;
    private VPosResponseUtils vPosResponseUtils;
    private HttpClient httpClient;
    private ClientsConfig clientsConfig;
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    public VposService(@Value("${vpos.requestUrl}") String vposUrl, PaymentRequestRepository paymentRequestRepository,
                       EcommerceClient ecommerceClient, VPosRequestUtils vPosRequestUtils, VPosResponseUtils vPosResponseUtils,
                       HttpClient httpClient, ClientsConfig clientsConfig, JwtTokenUtils jwtTokenUtils,
                       @Value("${vpos.polling.url}") String vposPollingUrl) {
        this.vposUrl = vposUrl;
        this.vposPollingUrl = vposPollingUrl;
        this.paymentRequestRepository = paymentRequestRepository;
        this.ecommerceClient = ecommerceClient;
        this.vPosRequestUtils = vPosRequestUtils;
        this.vPosResponseUtils = vPosResponseUtils;
        this.httpClient = httpClient;
        this.clientsConfig = clientsConfig;
        this.jwtTokenUtils = jwtTokenUtils;
    }

    public StepZeroResponse startCreditCardPayment(String clientId, String mdcFields, StepZeroRequest request) {
        setMdcFields(mdcFields);
        log.info("START - POST " + REQUEST_PAYMENTS_VPOS);

        if (!clientsConfig.containsKey(clientId)) {
            log.error(String.format("Client id %s is not valid", clientId));
            return createStepZeroResponse(BAD_REQUEST_MSG_CLIENT_ID, null);
        }

        if (ObjectUtils.anyNull(request) || request.getAmount().equals(BigInteger.ZERO)) {
            log.error(BAD_REQUEST_MSG);
            return createStepZeroResponse(BAD_REQUEST_MSG, null);
        }

        String idTransaction = request.getIdTransaction();
        log.info(String.format("START - POST %s for idTransaction %s", REQUEST_PAYMENTS_VPOS, idTransaction));
        if ((Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction)))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createStepZeroResponse(TRANSACTION_ALREADY_PROCESSED_MSG, null);
        }

        try {
            return processStepZero(request, clientId, mdcFields);
        } catch (Exception e) {
            log.error(String.format("Error while constructing requestBody for idTransaction %s, cause: %s - %s", idTransaction, e.getCause(), e.getMessage()));
            return createStepZeroResponse(GENERIC_ERROR_MSG + request.getIdTransaction(), null);
        }
    }

    private StepZeroResponse processStepZero(StepZeroRequest request, String clientId, String mdcFields) throws Exception {
        PaymentRequestEntity entity = createEntity(clientId, mdcFields, request.getIdTransaction(), request);
        Map<String, String> params = vPosRequestUtils.buildStepZeroRequestParams(request, entity.getGuid());
        executeStepZeroAuth(params, entity, request);
        return createStepZeroResponse(null, entity.getGuid());
    }

    @Async
    private void executeStepZeroAuth(Map<String, String> params, PaymentRequestEntity entity, StepZeroRequest pgsRequest) {

        try {
            String requestId = entity.getGuid();
            log.info("Calling VPOS - Step 0 - for requestId: " + requestId);
            HttpClientResponse clientResponse = callVPos(params);
            ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            boolean toAccount = checkResultCode(response, entity);
            if (BooleanUtils.isTrue(pgsRequest.getIsFirstPayment())) {
                log.info("RequestId {} is for a first payment with credit card. Reverting", requestId);
                executeRevert(entity, pgsRequest);
            } else if (toAccount) {
                executeAccount(entity, pgsRequest);
            }

            //If the resultCode is 25 or 26, the PATCH is not called
            if (!METHOD_CHALLENGE_CODES.contains(response.getResultCode())) {
                executePatchTransaction(entity, pgsRequest);
            }
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + CAUSE + e.getCause() + " - " + e.getMessage(), e);
        }
    }

    private void executeAccount(PaymentRequestEntity entity, StepZeroRequest pgsRequest) {
        try {
            log.info("Calling VPOS - Accounting - for requestId: " + entity.getGuid());
            Map<String, String> params = vPosRequestUtils.buildAccountingRequestParams(pgsRequest, entity.getCorrelationId());
            HttpClientResponse clientResponse = callVPos(params);
            AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            checkAccountResultCode(response, entity);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + CAUSE + e.getCause() + " - " + e.getMessage(), e);
        }
    }

    private void executeRevert(PaymentRequestEntity entity, StepZeroRequest pgsRequest) {
        try {
            log.info("Calling VPOS - Revert - for requestId: " + entity.getGuid());
            Map<String, String> params = vPosRequestUtils.buildRevertRequestParams(pgsRequest, entity.getCorrelationId());
            HttpClientResponse clientResponse = callVPos(params);
            AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            checkRevertResultCode(response, entity);
        } catch (Exception e) {
            log.error(GENERIC_REFUND_ERROR_MSG + entity.getIdTransaction() + CAUSE + e.getCause() + " - " + e.getMessage(), e);
        }
    }

    private void executePatchTransaction(PaymentRequestEntity entity, StepZeroRequest pgsRequest) throws IOException {
        String requestId = entity.getGuid();
        log.info("START - PATCH updateTransaction for requestId: " + requestId);
        AuthResultEnum authResult = entity.getStatus().equals(AUTHORIZED.name()) ? AuthResultEnum.OK : AuthResultEnum.KO;

        String authCode;
        if (AUTHORIZED.name().equals(entity.getStatus())) {
            authCode = entity.getAuthorizationCode();
        } else {
            authCode = entity.getErrorCode();
        }

        UpdateAuthRequest patchRequest = new UpdateAuthRequest(authResult, authCode);
        try {
            ClientConfig clientConfig = clientsConfig.getByKey(entity.getClientId());
            TransactionInfo patchResponse = ecommerceClient.callPatchTransaction(patchRequest, entity.getIdTransaction(), clientConfig);
            log.info(String.format("Response from PATCH updateTransaction for requestId %s is %s", requestId, patchResponse.toString()));
        } catch (Exception e) {
            log.error(PATCH_CLOSE_PAYMENT_ERROR + requestId, e);
            log.info("Refunding payment with requestId: " + requestId);
            if (executeOrderStatus(entity, pgsRequest).equals(OK)) {
                executeRevert(entity, pgsRequest);
            }

        }
    }

    private HttpClientResponse callVPos(Map<String, String> params) throws IOException {
        HttpClientResponse clientResponse = httpClient.post(vposUrl, ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), params);
        if (clientResponse.getStatus() != HttpStatus.OK.value()) {
            log.error("HTTP Response Status: " + clientResponse.getStatus());
            throw new IOException("Non-ok response from VPos. HTTP status: " + clientResponse.getStatus());
        }
        return clientResponse;
    }

    private StepZeroResponse createStepZeroResponse(String errorMessage, String requestId) {
        StepZeroResponse response = new StepZeroResponse();

        if (StringUtils.isNotBlank(requestId)) {
            response.setRequestId(requestId);
        }

        if (StringUtils.isEmpty(errorMessage)) {
            String sessionToken = jwtTokenUtils.generateToken(requestId);
            String urlRedirect = vposPollingUrl + requestId + "#token=" + sessionToken;
            response.setUrlRedirect(urlRedirect);
        } else {
            response.setError(errorMessage);
        }

        log.info(String.format("END - POST %s for requestId %s", REQUEST_PAYMENTS_VPOS, requestId));
        return response;
    }

    private PaymentRequestEntity createEntity(String clientId, String mdcFields, String idTransaction, StepZeroRequest request) throws JsonProcessingException {
        VposPersistableRequest vposPersistableRequest = generateVposDatabaseRequest(request);
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setClientId(clientId);
        entity.setMdcInfo(mdcFields);
        entity.setIdTransaction(idTransaction);
        entity.setGuid(UUID.randomUUID().toString());
        entity.setRequestEndpoint(REQUEST_PAYMENTS_VPOS);
        entity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        entity.setJsonRequest(vposPersistableRequest);
        return entity;
    }

    private VposPersistableRequest generateVposDatabaseRequest(StepZeroRequest request) {
        return new VposPersistableRequest(request.getIdTransaction(),
                request.getAmount(), request.getIsFirstPayment(), request.getIdPsp());
    }

    private boolean checkResultCode(ThreeDS2Response response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = CREATED.name();
        String responseType = "ERROR";
        String correlationId = StringUtils.EMPTY;
        String responseVposUrl = StringUtils.EMPTY;
        String errorCode = StringUtils.EMPTY;
        boolean isToAccount = false;
        switch (resultCode) {
            case RESULT_CODE_AUTHORIZED:
                responseType = response.getResponseType().name();
                isToAccount = true;
                correlationId = ((ThreeDS2Authorization) response.getThreeDS2ResponseElement()).getTransactionId();
                break;
            case RESULT_CODE_METHOD:
                ThreeDS2Method methodResponse = (ThreeDS2Method) response.getThreeDS2ResponseElement();
                responseType = response.getResponseType().name();
                responseVposUrl = methodResponse.getThreeDSMethodUrl();
                correlationId = methodResponse.getThreeDSTransId();
                break;
            case RESULT_CODE_CHALLENGE:
                ThreeDS2Challenge challengeResponse = ((ThreeDS2Challenge) response.getThreeDS2ResponseElement());
                responseType = response.getResponseType().name();
                responseVposUrl = getChallengeUrl(challengeResponse);
                correlationId = (challengeResponse.getThreeDSTransId());
                break;
            default:
                log.error(String.format("Error resultCode %s from Vpos for requestId %s", resultCode, entity.getGuid()));
                errorCode = resultCode;
                status = DENIED.name();
        }
        entity.setCorrelationId(correlationId);
        entity.setStatus(status);
        entity.setAuthorizationUrl(responseVposUrl);
        entity.setResponseType(responseType);
        entity.setErrorCode(errorCode);
        paymentRequestRepository.save(entity);
        return isToAccount;
    }

    private String getChallengeUrl(ThreeDS2Challenge threeDS2Challenge) {
        String url = threeDS2Challenge.getAcsUrl();
        String creq = threeDS2Challenge.getCReq();

        return url + "?creq=" + creq;
    }

    private void checkAccountResultCode(AuthResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = AUTHORIZED.name();
        String errorCode = StringUtils.EMPTY;
        boolean authorizationOutcome = true;
        if (!resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            status = DENIED.name();
            authorizationOutcome = false;
            errorCode = resultCode;
        }
        entity.setAuthorizationCode(response.getAuthorizationNumber());
        entity.setAuthorizationOutcome(authorizationOutcome);
        entity.setStatus(status);
        entity.setAuthorizationCode(response.getAuthorizationNumber());
        entity.setErrorCode(errorCode);
        paymentRequestRepository.save(entity);
        log.info("END - Vpos Request Payment Account for requestId " + entity.getGuid());
    }

    private void checkRevertResultCode(AuthResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        if (resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            entity.setStatus(CANCELLED.name());
            entity.setIsRefunded(true);
            paymentRequestRepository.save(entity);
        } else {
            entity.setErrorMessage("Error during Revert");
            entity.setIsRefunded(false);
        }
        log.info("END - VPos Request Payment Revert for requestId " + entity.getGuid() + " - resultCode: " + resultCode);
    }

    private RefundOutcome executeOrderStatus(PaymentRequestEntity entity, StepZeroRequest stepZeroRequest) throws IOException {
        log.info("Calling VPOS - OrderStatus - for requestId: " + entity.getGuid());
        Map<String, String> params = vPosRequestUtils.buildOrderStatusParams(stepZeroRequest);
        HttpClientResponse clientResponse = callVPos(params);
        VposOrderStatusResponse response = vPosResponseUtils.buildOrderStatusResponse(clientResponse.getEntity());
        return computeOrderStatusResultCode(response, entity);
    }

    private RefundOutcome computeOrderStatusResultCode(VposOrderStatusResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        if (resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            return OK;
        } else {
            entity.setErrorMessage("Error during orderStatus");
            paymentRequestRepository.save(entity);
            return KO;
        }
    }
}
