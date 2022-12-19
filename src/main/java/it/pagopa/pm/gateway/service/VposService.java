package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.PatchRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Challenge;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Method;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Response;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
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

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_CREDIT_CARD;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.constant.VposConstant.*;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.TX_AUTHORIZED_BY_PGS;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.TX_REFUSED;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@Service
@Slf4j
public class VposService {
    @Value("${vpos.response.urlredirect}")
    private String responseUrlRedirect;

    @Value("${vpos.requestUrl}")
    private String vposUrl;

    private static final List<String> VALID_CLIENT_ID = Arrays.asList("APP", "WEB");
    private static final String CAUSE = " cause: ";

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private RestapiCdClientImpl restapiCdClient;

    @Autowired
    private VPosRequestUtils vPosRequestUtils;

    @Autowired
    private VPosResponseUtils vPosResponseUtils;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;


    public StepZeroResponse startCreditCardPayment(String clientId, String mdcFields, StepZeroRequest request) {
        setMdcFields(mdcFields);
        log.info("START - POST " + REQUEST_PAYMENTS_CREDIT_CARD);
        if (!VALID_CLIENT_ID.contains(clientId)) {
            log.error(String.format("Client id %s is not valid", clientId));
            return createStepZeroResponse(BAD_REQUEST_MSG_CLIENT_ID, null);
        }

        if (ObjectUtils.anyNull(request) || request.getAmount().equals(BigInteger.ZERO)) {
            log.error(BAD_REQUEST_MSG);
            return createStepZeroResponse(BAD_REQUEST_MSG, null);
        }

        String idTransaction = request.getIdTransaction();
        log.info(String.format("START - POST %s for idTransaction %s", REQUEST_PAYMENTS_CREDIT_CARD, idTransaction));
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

    private StepZeroResponse processStepZero(StepZeroRequest request, String clientId, String mdcFields) throws IOException {
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
            if (BooleanUtils.isTrue(pgsRequest.getIsFirstPayment())) {
                log.info(String.format("RequestId %s is for a first payment with credit card. Reverting", requestId));
                executeRevert(entity, pgsRequest);
            } else if (checkResultCode(response, entity)) {
                executeAccount(entity, pgsRequest);
            }
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + CAUSE + e.getCause() + " - " + e.getMessage(), e);
        }
    }

    private void executeAccount(PaymentRequestEntity entity, StepZeroRequest pgsRequest) {
        try {
            log.info("Calling VPOS - Accounting - for requestId: " + entity.getGuid());
            Map<String, String> params = vPosRequestUtils.buildAccountingRequestParams(pgsRequest);
            HttpClientResponse clientResponse = callVPos(params);
            AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            checkAccountResultCode(response, entity);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + CAUSE + e.getCause() + " - " + e.getMessage(), e);
        }
        executePatchTransaction(entity);
    }

    private void executeRevert(PaymentRequestEntity entity, StepZeroRequest pgsRequest) {
        try {
            log.info("Calling VPOS - Revert - for requestId: " + entity.getGuid());
            Map<String, String> params = vPosRequestUtils.buildRevertRequestParams(pgsRequest);
            HttpClientResponse clientResponse = callVPos(params);
            AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            checkRevertResultCode(response, entity);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + CAUSE + e.getCause() + " - " + e.getMessage(), e);
        }
    }

    private void executePatchTransaction(PaymentRequestEntity entity) {
        String requestId = entity.getGuid();
        log.info("START - PATCH updateTransaction for requestId: " + requestId);
        Long transactionStatus = entity.getStatus().equals(AUTHORIZED.name()) ? TX_AUTHORIZED_BY_PGS.getId() : TX_REFUSED.getId();
        String authCode = entity.getAuthorizationCode();
        PatchRequest patchRequest = new PatchRequest(transactionStatus, authCode);
        try {
            String result = restapiCdClient.callPatchTransactionV2(Long.valueOf(entity.getIdTransaction()), patchRequest);
            log.info(String.format("Response from PATCH updateTransaction for requestId %s is %s", requestId, result));
        } catch (Exception e) {
            log.error(PATCH_CLOSE_PAYMENT_ERROR + requestId, e);
            log.info("Refunding payment with requestId: " + requestId);
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
            String urlRedirect = StringUtils.join(responseUrlRedirect, requestId);
            response.setUrlRedirect(urlRedirect);
            response.setStatus(CREATED.name());
        } else {
            response.setError(errorMessage);
        }

        log.info(String.format("END - POST %s for requestId %s", REQUEST_PAYMENTS_CREDIT_CARD, requestId));
        return response;
    }

    private PaymentRequestEntity createEntity(String clientId, String mdcFields, String idTransaction, StepZeroRequest request) throws JsonProcessingException {
        String requestJson = objectMapper.writeValueAsString(request);
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setClientId(clientId);
        entity.setMdcInfo(mdcFields);
        entity.setIdTransaction(idTransaction);
        entity.setGuid(UUID.randomUUID().toString());
        entity.setRequestEndpoint(REQUEST_PAYMENTS_CREDIT_CARD);
        entity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        entity.setJsonRequest(requestJson);
        return entity;
    }

    private boolean checkResultCode(ThreeDS2Response response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = CREATED.name();
        String responseType = StringUtils.EMPTY;
        String vposUrl = StringUtils.EMPTY;
        boolean isToAccount = false;
        switch (resultCode) {
            case RESULT_CODE_AUTHORIZED:
                responseType = response.getResponseType().name();
                isToAccount = true;
                break;
            case RESULT_CODE_METHOD:
                responseType = response.getResponseType().name();
                vposUrl = getMethodUrl(((ThreeDS2Method) response.getThreeDS2ResponseElement()));
                break;
            case RESULT_CODE_CHALLENGE:
                responseType = response.getResponseType().name();
                vposUrl = getChallengeUrl((ThreeDS2Challenge) response.getThreeDS2ResponseElement());
                break;
            default:
                log.error(String.format("Error resultCode %s from Vpos for requestId %s", resultCode, entity.getGuid()));
                status = DENIED.name();
        }
        entity.setStatus(status);
        entity.setAuthorizationUrl(vposUrl);
        entity.setResponseType(responseType);
        paymentRequestRepository.save(entity);
        return isToAccount;
    }

    private String getMethodUrl(ThreeDS2Method threeDS2Method) {
        String url = threeDS2Method.getThreeDSMethodUrl();
        String data = threeDS2Method.getThreeDSMethodData();

        return url + "?threeDSMethodData=" + data;
    }

    private String getChallengeUrl(ThreeDS2Challenge threeDS2Challenge) {
        String url = threeDS2Challenge.getAcsUrl();
        String creq = threeDS2Challenge.getCReq();

        return url + "?creq=" + creq;
    }

    private void checkAccountResultCode(AuthResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = AUTHORIZED.name();
        if (!resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            status = DENIED.name();
        }
        entity.setStatus(status);
        paymentRequestRepository.save(entity);
        log.info("END - XPay Request Payment Account for requestId " + entity.getGuid());
    }

    private void checkRevertResultCode(AuthResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        if (resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            entity.setStatus(CANCELLED.name());
            entity.setIsRefunded(true);
            paymentRequestRepository.save(entity);
        }
        log.info("END - XPay Request Payment Revert for requestId " + entity.getGuid());
    }

}
