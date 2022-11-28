package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.PatchRequest;
import it.pagopa.pm.gateway.dto.creditcard.Step0CreditCardRequest;
import it.pagopa.pm.gateway.dto.creditcard.Step0CreditCardResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_CREDIT_CARD;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.TX_AUTHORIZED_BY_PGS;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.TX_REFUSED;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@Service
@Slf4j
public class CCRequestPaymentsService {

    @Value("${vpos.response.urlredirect}")
    private String responseUrlRedirect;

    @Value("${vpos.requestUrl}")
    private String vposUrl;

    private static final String APP_ORIGIN = "APP";
    private static final String WEB_ORIGIN = "WEB";
    private static final List<String> VALID_CLIENT_ID = Arrays.asList(APP_ORIGIN, WEB_ORIGIN);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String RESULT_CODE_AUTHORIZED = "00";
    private static final String RESULT_CODE_METHOD = "25";
    private static final String RESULT_CODE_CHALLENGE = "26";
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

    public ResponseEntity<Step0CreditCardResponse> getRequestPayments(String clientId, String mdcFields, Step0CreditCardRequest request) {
        if (!VALID_CLIENT_ID.contains(clientId)) {
            log.info("START - POST " + REQUEST_PAYMENTS_CREDIT_CARD);
            log.error(String.format("Client id %s is not valid", clientId));
            return createStep0CreditCardResponse(BAD_REQUEST_MSG_CLIENT_ID, HttpStatus.BAD_REQUEST, null);
        }

        if (ObjectUtils.anyNull(request) || request.getAmount().equals(BigInteger.ZERO)) {
            log.info("START - POST " + REQUEST_PAYMENTS_CREDIT_CARD);
            log.error(BAD_REQUEST_MSG);
            return createStep0CreditCardResponse(BAD_REQUEST_MSG, HttpStatus.BAD_REQUEST, null);
        }

        String idTransaction = request.getIdTransaction();
        log.info(String.format("START - POST %s for idTransaction %s", REQUEST_PAYMENTS_CREDIT_CARD, idTransaction));
        setMdcFields(mdcFields);

        if ((Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction)))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createStep0CreditCardResponse(TRANSACTION_ALREADY_PROCESSED_MSG, HttpStatus.UNAUTHORIZED, null);
        }

        ResponseEntity<Step0CreditCardResponse> response;
        try {
            response = createStep0AuthPaymentCreditCard(request, clientId, mdcFields);
        } catch (Exception e) {
            log.error(String.format("Error constructing requestBody for idTransaction %s, cause: %s - %s", idTransaction, e.getCause(), e.getMessage()));
            return createStep0CreditCardResponse(GENERIC_ERROR_MSG + request.getIdTransaction(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
        return response;
    }

    private ResponseEntity<Step0CreditCardResponse> createStep0AuthPaymentCreditCard(Step0CreditCardRequest request, String clientId, String mdcFields) throws IOException {
        String idTransaction = request.getIdTransaction();

        PaymentRequestEntity entity = generateEntity(clientId, mdcFields, idTransaction, request);
        Map<String, String> params = vPosRequestUtils.generateRequestForStep0(request, entity.getGuid());
        executeStep0(params, entity, request);
        return createStep0CreditCardResponse(null, HttpStatus.OK, entity.getGuid());
    }

    @Async
    private void executeStep0(Map<String, String> params, PaymentRequestEntity entity, Step0CreditCardRequest pgsRequest) {
        ThreeDS2Response response;
        try {
            log.info("Calling VPOS - Step 0 - for requestId: " + entity.getGuid());
            HttpClientResponse clientResponse = callVPos(params);
            response = vPosResponseUtils.build3ds2Response(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac3ds2(response, pgsRequest);
            if (BooleanUtils.isTrue(pgsRequest.getIsFirstPayment())) {
                executeRevert(entity, pgsRequest);
            } else {
                Boolean isToAccount = checkResultCode(response, entity);
                if (BooleanUtils.isTrue(isToAccount)) {
                    executeAccount(entity, pgsRequest);
                }
            }
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + CAUSE + e.getCause() + " - " + e.getMessage(), e);
        }
    }

    @Async
    private void executeAccount(PaymentRequestEntity entity, Step0CreditCardRequest pgsRequest) {
        AuthResponse response;
        try {
            log.info("Calling VPOS - Accounting - for requestId: " + entity.getGuid());
            Map<String, String> params = vPosRequestUtils.generateRequestForAccount(pgsRequest);
            HttpClientResponse clientResponse = callVPos(params);
            response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response, pgsRequest);
            checkAccountResultCode(response, entity);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + CAUSE + e.getCause() + " - " + e.getMessage(), e);
        }
        executePatchTransaction(entity);
    }

    private void executeRevert(PaymentRequestEntity entity, Step0CreditCardRequest pgsRequest) {
        AuthResponse response;
        try {
            log.info("Calling VPOS - Revert - for requestId: " + entity.getGuid());
            Map<String, String> params = vPosRequestUtils.generateRequestForRevert(pgsRequest);
            HttpClientResponse clientResponse = callVPos(params);
            response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response, pgsRequest);
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

    private ResponseEntity<Step0CreditCardResponse> createStep0CreditCardResponse(String errorMessage, HttpStatus httpStatus, String requestId) {
        Step0CreditCardResponse response = new Step0CreditCardResponse();

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
        return ResponseEntity.status(httpStatus).body(response);
    }

    private PaymentRequestEntity generateEntity(String clientId, String mdcFields, String idTransaction, Step0CreditCardRequest request) throws JsonProcessingException {
        String requestJson = OBJECT_MAPPER.writeValueAsString(request);
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

    private Boolean checkResultCode(ThreeDS2Response response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = CREATED.name();
        String responseType = StringUtils.EMPTY;
        String acsUrl = StringUtils.EMPTY;
        boolean isToAccount = false;
        switch (resultCode) {
            case RESULT_CODE_AUTHORIZED:
                responseType = response.getResponseType().name();
                isToAccount = true;
                break;
            case RESULT_CODE_METHOD:
                responseType = response.getResponseType().name();
                acsUrl = ((ThreeDS2Method) response.getThreeDS2ResponseElement()).getThreeDSMethodUrl();
                break;
            case RESULT_CODE_CHALLENGE:
                responseType = response.getResponseType().name();
                acsUrl = ((ThreeDS2Challenge) response.getThreeDS2ResponseElement()).getAcsUrl();
                break;
            default:
                log.error(String.format("Error resultCode %s from Vpos for requestId %s", resultCode, entity.getGuid()));
                status = DENIED.name();
        }
        entity.setStatus(status);
        entity.setAuthorizationUrl(acsUrl);
        entity.setResponseType(responseType);
        paymentRequestRepository.save(entity);
        return isToAccount;
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
