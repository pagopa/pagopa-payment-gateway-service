package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.dto.PatchRequest;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.xpay.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.XpayService;
import it.pagopa.pm.gateway.utils.XPayUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.net.URI;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.Headers.X_CLIENT_ID;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.constant.XPayParams.*;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.TX_AUTHORIZED_BY_PGS;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.TX_REFUSED;
import static it.pagopa.pm.gateway.dto.xpay.EsitoXpay.KO;
import static it.pagopa.pm.gateway.dto.xpay.EsitoXpay.OK;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
@RequestMapping(REQUEST_PAYMENTS_XPAY)
public class XPayPaymentController {

    private static final String APP_ORIGIN = "APP";
    private static final String WEB_ORIGIN = "WEB";
    private static final List<String> VALID_CLIENT_ID = Arrays.asList(APP_ORIGIN, WEB_ORIGIN);
    public static final String EUR_CURRENCY = "978";
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String ZERO_CHAR = "0";
    private static final int MAX_RETRIES = 3;

    @Value("${xpay.response.urlredirect}")
    private String responseUrlRedirect;

    @Value("${xpay.request.responseUrl}")
    private String xpayResponseUrl;

    @Value("${xpay.apiKey}")
    private String apiKey;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private XpayService xpayService;

    @Autowired
    private RestapiCdClientImpl restapiCdClient;

    @Autowired
    private XPayUtils xPayUtils;

    @PostMapping()
    public ResponseEntity<XPayAuthResponse> requestPaymentsXPay(@RequestHeader(value = X_CLIENT_ID) String clientId,
                                                                @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                @RequestBody XPayAuthRequest pgsRequest) {
        if (!VALID_CLIENT_ID.contains(clientId)) {
            log.info("START - POST " + REQUEST_PAYMENTS_XPAY);
            log.error(String.format("Client id %s is not valid", clientId));
            return createXpayAuthResponse(BAD_REQUEST_MSG_CLIENT_ID, HttpStatus.BAD_REQUEST, null);
        }

        if (ObjectUtils.anyNull(pgsRequest) || pgsRequest.getGrandTotal().equals(BigInteger.ZERO)) {
            log.info("START POST - " + REQUEST_PAYMENTS_XPAY);
            log.error(BAD_REQUEST_MSG);
            return createXpayAuthResponse(BAD_REQUEST_MSG, HttpStatus.BAD_REQUEST, null);
        }

        String idTransaction = pgsRequest.getIdTransaction();
        log.info(String.format("START - POST %s for idTransaction %s", REQUEST_PAYMENTS_XPAY, idTransaction));
        setMdcFields(mdcFields);

        if (Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createXpayAuthResponse(TRANSACTION_ALREADY_PROCESSED_MSG, HttpStatus.UNAUTHORIZED, null);
        }

        return createAuthPaymentXpay(pgsRequest, clientId, mdcFields);
    }

    @GetMapping(REQUEST_ID)
    public ResponseEntity<XPayPollingResponse> getRequestInfo(@PathVariable String requestId,
                                                              @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) {
        log.info("START - GET XPay request info for requestId: " + requestId);
        setMdcFields(mdcFields);
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
        if (Objects.isNull(entity) || !StringUtils.equals(entity.getRequestEndpoint(), REQUEST_PAYMENTS_XPAY)) {
            log.error("No XPay request entity has been found for requestId: " + requestId);
            XPayPollingResponseError error = new XPayPollingResponseError(404L, REQUEST_ID_NOT_FOUND_MSG);
            return createXPayAuthPollingResponse(HttpStatus.NOT_FOUND, error, null);
        }
        return createXPayAuthPollingResponse(HttpStatus.OK, null, entity);
    }

    @GetMapping(XPAY_RESUME)
    public ResponseEntity<String> resumeXPayPayment(@PathVariable String requestId,
                                                    @RequestParam Map<String, String> params) throws JsonProcessingException {

        log.info(String.format("START - GET %s for requestId %s", REQUEST_PAYMENTS_XPAY + XPAY_RESUME, requestId));
        log.info("Params received from XPay: " + params);

        if (ObjectUtils.anyNull(params, params.get(XPAY_OUTCOME))) {
            log.error(BAD_REQUEST_MSG + " for XPay resume request - requestId " + requestId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BAD_REQUEST_MSG);
        }

        XPay3DSResponse xPay3DSResponse = buildXPay3DSResponse(params);
        EsitoXpay outcome = xPay3DSResponse.getOutcome();

        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
        if (Objects.isNull(entity)) {
            log.error("No XPay entity has been found for requestId: " + requestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(REQUEST_ID_NOT_FOUND_MSG);
        }

        if (Objects.nonNull(entity.getAuthorizationOutcome())) {
            log.warn(String.format("requestId %s already processed", requestId));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(TRANSACTION_ALREADY_PROCESSED_MSG);
        }

        if (outcome.equals(OK)) {
            String xPayMac = xPay3DSResponse.getMac();
            if (BooleanUtils.isFalse(xPayUtils.checkMac(entity, xPayMac))) {
                log.error(String.format(MAC_NOT_EQUAL_ERROR_MSG, xPayMac) + "for requestId: " + requestId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MAC_NOT_EQUAL_ERROR_MSG);
            }
            executeXPayPaymentCall(requestId, xPay3DSResponse, entity);
            executePatchTransactionV2(entity, requestId);
        } else {
            log.info(String.format("Outcome is %s: setting status as DENIED for requestId %s", outcome, requestId));
            entity.setStatus(DENIED.name());
            paymentRequestRepository.save(entity);
        }

        String urlRedirect = StringUtils.join(responseUrlRedirect, requestId);
        log.info(String.format("END - GET %s for requestId %s", REQUEST_PAYMENTS_XPAY + XPAY_RESUME, requestId));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(urlRedirect)).build();
    }

    private ResponseEntity<XPayAuthResponse> createXpayAuthResponse(String errorMessage, HttpStatus status, String requestId) {
        XPayAuthResponse response = new XPayAuthResponse();
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

        log.info(String.format("END - POST %s for requestId %s", REQUEST_PAYMENTS_XPAY, requestId));
        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping(REQUEST_ID)
    public ResponseEntity<XPayRefundResponse> xPayRefund(@PathVariable String requestId) {

        log.info("START - requesting XPay refund for requestId: " + requestId);

        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);

        if (Objects.isNull(entity)) {
            log.error(REQUEST_ID_NOT_FOUND_MSG);
            return createXPayRefundRespone(requestId, HttpStatus.NOT_FOUND, null);
        }

        if (BooleanUtils.isTrue(entity.getIsRefunded())) {
            log.info("RequestId " + requestId + " has been refunded already. Skipping refund");
            return createXPayRefundRespone(requestId, HttpStatus.OK, null);
        }

        EsitoXpay refundOutcome = null;
        try {
            EsitoXpay outcomeOrderStatus = executeXPayOrderStatus(entity);
            if (outcomeOrderStatus.equals(OK)) {
                refundOutcome = executeXPayRevert(entity);
            }
        } catch (Exception e) {
            return createXPayRefundRespone(requestId, HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
        return createXPayRefundRespone(requestId, HttpStatus.OK, refundOutcome);
    }

    private ResponseEntity<XPayAuthResponse> createAuthPaymentXpay(XPayAuthRequest pgsRequest, String clientId, String mdcFields) {
        String transactionId = pgsRequest.getIdTransaction();
        log.info("START - requesting XPay payment authorization for transactionId " + transactionId);

        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        AuthPaymentXPayRequest xPayAuthRequest = createXpayAuthRequest(pgsRequest);
        generateRequestEntity(clientId, mdcFields, transactionId, paymentRequestEntity, xPayAuthRequest);
        xPayAuthRequest.setUrlRisposta(String.format(xpayResponseUrl, paymentRequestEntity.getGuid()));
        executeXPayAuthorizationCall(xPayAuthRequest, paymentRequestEntity, transactionId);

        return createXpayAuthResponse(null, HttpStatus.OK, paymentRequestEntity.getGuid());
    }

    @Async
    private void executeXPayAuthorizationCall(AuthPaymentXPayRequest xPayRequest, PaymentRequestEntity requestEntity, String transactionId) {
        log.info("START - execute XPay payment authorization call for transactionId: " + transactionId);
        try {
            AuthPaymentXPayResponse response = xpayService.callAutenticazione3DS(xPayRequest);
            if (ObjectUtils.isEmpty(response)) {
                String errorMsg = "Response from XPay to /autenticazione3DS is empty";
                log.error(errorMsg);
                requestEntity.setStatus(DENIED.name());
            } else {
                requestEntity.setTimeStamp(String.valueOf(response.getTimeStamp()));
                XpayError xpayError = response.getErrore();
                if (ObjectUtils.isEmpty(xpayError)) {
                    requestEntity.setXpayHtml(response.getHtml());
                } else {
                    requestEntity.setErrorCode(String.valueOf(xpayError.getCodice()));
                    requestEntity.setErrorMessage(xpayError.getMessaggio());
                    requestEntity.setStatus(DENIED.name());
                }
                paymentRequestRepository.save(requestEntity);
                log.info("END - XPay Request Payment Authorization for idTransaction " + transactionId);
            }
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + transactionId + " cause: " + e.getCause() + " - " + e.getMessage(), e);
        }
    }

    private ResponseEntity<XPayPollingResponse> createXPayAuthPollingResponse(HttpStatus httpStatus, XPayPollingResponseError error, PaymentRequestEntity entity) {
        XPayPollingResponse response = new XPayPollingResponse();

        if (Objects.nonNull(error)) {
            log.info("START - create XPay polling response - error case");
            response.setError(error);
            return ResponseEntity.status(httpStatus).body(response);
        }

        String requestId = entity.getGuid();
        response.setRequestId(requestId);

        log.info("START - create XPay polling response for requestId: " + requestId);
        String status = entity.getStatus();
        log.info(String.format("Request status for requestId %s is %s", requestId, status));

        response.setStatus(status);
        PaymentRequestStatusEnum statusEnum = getEnumValueFromString(status);
        switch (statusEnum) {
            case CREATED:
                String xpayHtml = entity.getXpayHtml();
                response.setHtml(xpayHtml);
                if (StringUtils.isBlank(xpayHtml)) {
                    log.info(String.format("HTML from XPay for requestId %s has not been acquired yet", requestId));
                }
                break;
            case AUTHORIZED:
            case DENIED:
                String authOutcome = BooleanUtils.toBoolean(entity.getAuthorizationOutcome()) ? OK.name() : KO.name();
                log.info(String.format("Authorization outcome for requestId %s is %s", requestId, authOutcome));
                response.setAuthOutcome(authOutcome);
                response.setAuthCode(entity.getAuthorizationCode());
                response.setRedirectUrl(StringUtils.join(responseUrlRedirect, requestId));
                break;
            default:
                log.info(BooleanUtils.toBoolean(entity.getIsRefunded()) ?
                        String.format("XPay request with requestId %s has been refunded", requestId) :
                        String.format("XPay request with requestId %s has not been refunded yet", requestId));
                break;
        }

        if (ObjectUtils.allNotNull(entity.getErrorCode(), entity.getErrorMessage())) {
            response.setError(new XPayPollingResponseError(Long.valueOf(entity.getErrorCode()), entity.getErrorMessage()));
            return ResponseEntity.ok().body(response);
        }

        log.info("END - create XPay polling response for requestId " + requestId);
        return ResponseEntity.ok().body(response);
    }

    @Async
    private void executeXPayPaymentCall(String requestId, XPay3DSResponse xpay3DSResponse, PaymentRequestEntity entity) {
        log.info("START - executeXPayPaymentCall for requestId " + requestId);
        entity.setXpayNonce(xpay3DSResponse.getXpayNonce());
        String status = DENIED.name();
        int retryCount = 1;
        boolean isAuthorized = false;
        log.info("Calling XPay /paga3DS - requestId: " + requestId);
        while (!isAuthorized && retryCount <= MAX_RETRIES) {
            try {
                PaymentXPayRequest xpayRequest = createXPayPaymentRequest(requestId, entity);
                log.info(String.format("Attempt no.%s for requestId: %s", retryCount, requestId));
                PaymentXPayResponse response = xpayService.callPaga3DS(xpayRequest);
                if (ObjectUtils.isEmpty(response)) {
                    log.warn(String.format("paga3DS response from XPay to requestId %s is empty", requestId));
                    retryCount++;
                } else {
                    EsitoXpay outcome = response.getEsito();
                    String logMsg = "paga3DS outcome for requestId %s is %s";
                    if (outcome == OK) {
                        log.info(String.format(logMsg, requestId, OK.name()));
                        isAuthorized = true;
                        entity.setStatus(AUTHORIZED.name());
                        entity.setAuthorizationCode(response.getCodiceAutorizzazione());
                    } else if (outcome == KO) {
                        log.warn(String.format(logMsg, requestId, KO.name()));
                        entity.setStatus(DENIED.name());
                        setErrorCodeAndMessage(requestId, entity, response);
                        retryCount++;
                    }
                }
            } catch (Exception e) {
                log.error(String.format("An exception occurred while calling XPay's /paga3DS for requestId: %s. " +
                        "Cause: %s, message: %s", requestId, e.getCause(), e.getMessage()));
                log.error("Complete exception:", e);
                retryCount++;
            }
        }
        entity.setTimeStamp(xpay3DSResponse.getTimestamp());
        entity.setStatus(status);
        entity.setAuthorizationOutcome(isAuthorized);
        paymentRequestRepository.save(entity);
        log.info(String.format("END - executeXPayPaymentCall for requestId: %s. Status: %s " +
                "- Authorization: %s. Retry attempts number: %s", requestId, entity.getStatus(), isAuthorized, retryCount));
    }

    private void setErrorCodeAndMessage(String requestId, PaymentRequestEntity entity, PaymentXPayResponse response) {
        if (ObjectUtils.isNotEmpty(response.getErrore())) {
            XpayError xpayError = response.getErrore();
            String errorCode = String.valueOf(xpayError.getCodice());
            String errorMessage = xpayError.getMessaggio();
            log.info(String.format("RequestId %s has error code: %s - message: %s", requestId,
                    errorCode, errorMessage));
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(errorMessage);
        }
    }

    private void executePatchTransactionV2(PaymentRequestEntity entity, String requestId) {
        log.info("START - PATCH updateTransaction for requestId: " + requestId);
        Long transactionStatus = entity.getStatus().equals(AUTHORIZED.name()) ? TX_AUTHORIZED_BY_PGS.getId() : TX_REFUSED.getId();
        String authCode = entity.getAuthorizationCode();
        PatchRequest patchRequest = new PatchRequest(transactionStatus, authCode);
        try {
            String result = restapiCdClient.callPatchTransactionV2(Long.valueOf(entity.getIdTransaction()), patchRequest);
            log.info(String.format("Response from PATCH updateTransaction for requestId %s is %s", requestId, result));
        } catch (Exception e) {
            log.error(PATCH_CLOSE_PAYMENT_ERROR + requestId, e);
            entity.setStatus(CANCELLED.name());
        }
        paymentRequestRepository.save(entity);
    }

    private XPay3DSResponse buildXPay3DSResponse(Map<String, String> params) {
        log.info("Building XPay3DSResponse ");
        XPay3DSResponse xPay3DSResponse = new XPay3DSResponse();
        if (params.get(XPAY_KEY_RESUME_TYPE).equalsIgnoreCase(RESUME_TYPE_XPAY)) {
            xPay3DSResponse.setOutcome(EsitoXpay.valueOf(params.get(XPAY_OUTCOME)));
            xPay3DSResponse.setOperationId(params.get(XPAY_OPERATION_ID));
            xPay3DSResponse.setTimestamp(params.get(XPAY_TIMESTAMP));
            xPay3DSResponse.setMac(params.get(XPAY_MAC));
            xPay3DSResponse.setXpayNonce(params.get(XPAY_NONCE));
            xPay3DSResponse.setErrorCode(params.get(XPAY_ERROR_CODE));
            xPay3DSResponse.setErrorMessage(params.get(XPAY_ERROR_MESSAGE));
        }
        return xPay3DSResponse;
    }

    private PaymentXPayRequest createXPayPaymentRequest(String requestId, PaymentRequestEntity entity) throws JsonProcessingException {
        String idTransaction = entity.getIdTransaction();
        String codTrans = StringUtils.leftPad(idTransaction, 2, ZERO_CHAR);

        BigInteger grandTotal = xPayUtils.getGrandTotalForMac(entity);
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String mac = xPayUtils.createMac(codTrans, grandTotal, timeStamp);

        PaymentXPayRequest request = new PaymentXPayRequest();
        request.setDivisa(Long.valueOf(EUR_CURRENCY));
        request.setApiKey(apiKey);
        request.setCodiceTransazione(codTrans);
        request.setTimeStamp(timeStamp);
        request.setMac(mac);
        request.setImporto(grandTotal);
        request.setXpayNonce(entity.getXpayNonce());
        log.info("XPay payment request object created for requestId: " + requestId);
        return request;
    }

    private EsitoXpay executeXPayOrderStatus(PaymentRequestEntity entity) {
        String requestId = entity.getGuid();
        XPayOrderStatusResponse response;
        try {
            log.info("START executeXPayOrderStatus for requestId " + requestId);
            XPayOrderStatusRequest request = createXPayOrderStatusRequest(entity);
            response = xpayService.callSituazioneOrdine(request);
        } catch (Exception e) {
            log.error(GENERIC_REFUND_ERROR_MSG + requestId, e);
            throw e;
        }
        return response.getEsito();
    }

    private EsitoXpay executeXPayRevert(PaymentRequestEntity entity) throws Exception {
        String requestId = entity.getGuid();
        XPayRevertResponse response;
        try {
            log.info("START executeXPayOrderStatus for requestId " + requestId);
            XPayRevertRequest request = createXPayRevertRequest(entity);
            response = xpayService.callStorna(request);
        } catch (Exception e) {
            log.error(GENERIC_REFUND_ERROR_MSG + requestId, e);
            throw e;
        }
        if (response.getEsito().equals(OK)) {
            entity.setStatus(CANCELLED.name());
            entity.setIsRefunded(Boolean.TRUE);
            paymentRequestRepository.save(entity);
        }
        return response.getEsito();
    }

    private ResponseEntity<XPayRefundResponse> createXPayRefundRespone(String requestId, HttpStatus httpStatus, EsitoXpay refundOutcome) {
        XPayRefundResponse response = new XPayRefundResponse();
        response.setRequestId(requestId);

        if (httpStatus.is4xxClientError()) {
            response.setError(REQUEST_ID_NOT_FOUND_MSG);
        } else if (httpStatus.is2xxSuccessful()) {
            if (Objects.isNull(refundOutcome)) {
                response.setError("RequestId " + requestId + " has been refunded already. Skipping refund");
            } else {
                response.setRefundOutcome(String.valueOf(refundOutcome));
            }
        } else {
            response.setError(GENERIC_REFUND_ERROR_MSG + requestId);
        }

        log.info("END - requesting XPay refund for requestId: " + requestId);
        return ResponseEntity.status(httpStatus).body(response);
    }

    private XPayOrderStatusRequest createXPayOrderStatusRequest(PaymentRequestEntity entity) {
        String idTransaction = entity.getIdTransaction();
        String codTrans = StringUtils.leftPad(idTransaction, 2, ZERO_CHAR);

        String timeStamp = String.valueOf(System.currentTimeMillis());
        String mac = xPayUtils.createMacForRevert(codTrans, timeStamp);

        XPayOrderStatusRequest request = new XPayOrderStatusRequest();
        request.setApiKey(apiKey);
        request.setMac(mac);
        request.setCodiceTransazione(codTrans);
        request.setTimeStamp(timeStamp);

        return request;
    }

    private XPayRevertRequest createXPayRevertRequest(PaymentRequestEntity entity) throws JsonProcessingException {
        String idTransaction = entity.getIdTransaction();
        String codTrans = StringUtils.leftPad(idTransaction, 2, ZERO_CHAR);

        String json = entity.getJsonRequest();
        AuthPaymentXPayRequest authRequest = OBJECT_MAPPER.readValue(json, AuthPaymentXPayRequest.class);

        BigInteger grandTotal = authRequest.getImporto();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String mac = xPayUtils.createMacForRevert(codTrans, timeStamp);

        XPayRevertRequest request = new XPayRevertRequest();
        request.setApiKey(apiKey);
        request.setMac(mac);
        request.setCodiceTransazione(codTrans);
        request.setTimeStamp(timeStamp);
        request.setDivisa(Long.valueOf(EUR_CURRENCY));
        request.setImporto(grandTotal);

        return request;
    }

    private AuthPaymentXPayRequest createXpayAuthRequest(XPayAuthRequest pgsRequest) {
        String idTransaction = pgsRequest.getIdTransaction();
        String codTrans = StringUtils.leftPad(idTransaction, 2, ZERO_CHAR);
        String timeStamp = String.valueOf(System.currentTimeMillis());
        BigInteger grandTotal = pgsRequest.getGrandTotal();
        String mac = xPayUtils.createMac(codTrans, grandTotal, timeStamp);

        AuthPaymentXPayRequest xPayRequest = new AuthPaymentXPayRequest();
        xPayRequest.setApiKey(apiKey);
        xPayRequest.setImporto(grandTotal);
        xPayRequest.setCvv(pgsRequest.getCvv());
        xPayRequest.setPan(pgsRequest.getPan());
        xPayRequest.setDivisa(EUR_CURRENCY);
        xPayRequest.setMac(mac);
        xPayRequest.setScadenza(pgsRequest.getExpiryDate());
        xPayRequest.setTimeStamp(timeStamp);
        xPayRequest.setCodiceTransazione(codTrans);
        log.info("Request body to call autenticazione3DS created for transactionId " + idTransaction);
        return xPayRequest;
    }

    private void generateRequestEntity(String clientId, String mdcFields, String transactionId,
                                       PaymentRequestEntity paymentRequestEntity, AuthPaymentXPayRequest request) {
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_XPAY);
        paymentRequestEntity.setIdTransaction(transactionId);
        paymentRequestEntity.setMdcInfo(mdcFields);
        paymentRequestEntity.setTimeStamp(request.getTimeStamp());
        paymentRequestEntity.setStatus(CREATED.name());
        String jsonRequest = StringUtils.EMPTY;
        try {
            jsonRequest = OBJECT_MAPPER.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Error while serializing request as JSON. Request object is: " + request);
        }
        paymentRequestEntity.setJsonRequest(jsonRequest);
    }

}