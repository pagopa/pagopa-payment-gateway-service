package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.xpay.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.XpayService;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.Headers.X_CLIENT_ID;
import static it.pagopa.pm.gateway.constant.Messages.*;
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
    private static final String EUR_CURRENCY = "978";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ZERO_CHAR = "0";
    private static final int PAGA3DS_MAX_RETRIES = 3;

    @Value("${xpay.response.urlredirect}")
    private String responseUrlRedirect;

    @Value("${xpay.request.responseUrl}")
    private String xpayResponseUrl;

    @Value("${xpay.apiKey}")
    private String apiKey;

    @Value("${xpay.secretKey}")
    private String secretKey;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private XpayService xpayService;

    @Autowired
    private RestapiCdClientImpl restapiCdClient;

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

    @PostMapping(XPAY_RESUME)
    public ResponseEntity<String> resumeXPayPayment(@PathVariable String requestId,
                                                    @RequestBody XPayResumeRequest pgsRequest) {

        log.info(String.format("START - POST %s for requestId %s", REQUEST_PAYMENTS_XPAY + XPAY_RESUME, requestId));

        EsitoXpay outcome = pgsRequest.getEsito();
        if (Objects.isNull(outcome)) {
            log.error(BAD_REQUEST_MSG + " for XPay resume request - requestId " + requestId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BAD_REQUEST_MSG);
        }

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
            try {
                executeXPayPaymentCall(requestId, pgsRequest, entity);
                executePatchTransactionV2(entity, requestId);
            } catch (Exception e) {
                String errorMessage = String.format("An error occurred during payment for requestId: %s - reason: %s",
                        requestId, e.getMessage());
                log.error(errorMessage, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
            }
        } else {
            log.info(String.format("Outcome is %s: setting status as DENIED for requestId %s", outcome, requestId));
            entity.setStatus(DENIED.name());
            paymentRequestRepository.save(entity);
        }

        String urlRedirect = StringUtils.join(responseUrlRedirect, requestId);
        log.info(String.format("END - POST %s for requestId %s", REQUEST_PAYMENTS_XPAY + XPAY_RESUME, requestId));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(urlRedirect)).build();
    }

    private ResponseEntity<XPayAuthResponse> createXpayAuthResponse(String errorMessage, HttpStatus status, String requestId) {
        XPayAuthResponse response = new XPayAuthResponse();
        if (Objects.nonNull(requestId)) {
            PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
            response.setRequestId(requestId);
            response.setStatus(entity.getStatus());
        }
        if (StringUtils.isEmpty(errorMessage)) {
            String urlRedirect = StringUtils.join(this.responseUrlRedirect, requestId);
            response.setUrlRedirect(urlRedirect);
        } else {
            response.setError(errorMessage);
        }

        log.info(String.format("END - POST %s for requestId %s", REQUEST_PAYMENTS_XPAY, requestId));
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<XPayAuthResponse> createAuthPaymentXpay(XPayAuthRequest pgsRequest, String clientId, String mdcFields) {
        String transactionId = pgsRequest.getIdTransaction();
        log.info("START - requesting XPay payment authorization for transactionId " + transactionId);
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        AuthPaymentXPayRequest xPayAuthRequest = createXpayAuthRequest(pgsRequest);
        generateRequestEntity(clientId, mdcFields, transactionId, paymentRequestEntity, xPayAuthRequest);
        xPayAuthRequest.setUrlRisposta(String.format(xpayResponseUrl, paymentRequestEntity.getGuid()));
        return executeXPayAuthorizationCall(xPayAuthRequest, paymentRequestEntity, transactionId);
    }

    @Async
    private ResponseEntity<XPayAuthResponse> executeXPayAuthorizationCall(AuthPaymentXPayRequest xPayRequest, PaymentRequestEntity requestEntity, String transactionId) {
        log.info("START - execute XPay payment authorization call for transactionId: " + transactionId);
        try {
            AuthPaymentXPayResponse response = xpayService.callAutenticazione3DS(xPayRequest);
            if (ObjectUtils.isEmpty(response)) {
                String errorMsg = "Response from XPay to /autenticazione3DS is empty";
                log.error(errorMsg);
                return createXpayAuthResponse(errorMsg, HttpStatus.OK, null);
            } else {
                requestEntity.setTimeStamp(String.valueOf(response.getTimeStamp()));
                XpayError xpayError = response.getErrore();
                if (ObjectUtils.isEmpty(xpayError)) {
                    requestEntity.setXpayHtml(response.getHtml());
                    requestEntity.setAuthorizationOutcome(true);
                } else {
                    requestEntity.setErrorCode(String.valueOf(xpayError.getCodice()));
                    requestEntity.setErrorMessage(xpayError.getMessaggio());
                    requestEntity.setAuthorizationOutcome(false);
                    requestEntity.setStatus(DENIED.name());
                }
                paymentRequestRepository.save(requestEntity);
                log.info("END - XPay Request Payment Authorization for idTransaction " + transactionId);
                return createXpayAuthResponse(null, HttpStatus.OK, requestEntity.getGuid());
            }
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + transactionId + " cause: " + e.getCause() + " - " + e.getMessage(), e);
            return createXpayAuthResponse(GENERIC_ERROR_MSG + transactionId, HttpStatus.INTERNAL_SERVER_ERROR, null);
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
    private void executeXPayPaymentCall(String requestId, XPayResumeRequest pgsRequest, PaymentRequestEntity entity) throws JsonProcessingException {
        log.info("START - executeXPayPaymentCall for requestId " + requestId);
        PaymentXPayRequest xpayRequest = createXPayPaymentRequest(requestId, entity);
        int retryCount = 1;
        boolean isAuthorized = false;
        log.info("Calling XPay /paga3DS - requestId: " + requestId);
        while (!isAuthorized && retryCount <= PAGA3DS_MAX_RETRIES) {
            try {
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
        entity.setXpayNonce(pgsRequest.getXpayNonce());
        entity.setTimeStamp(pgsRequest.getTimestamp());
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
        }
    }

    private PaymentXPayRequest createXPayPaymentRequest(String requestId, PaymentRequestEntity entity) throws JsonProcessingException {
        String idTransaction = entity.getIdTransaction();
        String codTrans = StringUtils.leftPad(idTransaction, 2, ZERO_CHAR);

        String json = entity.getJsonRequest();
        AuthPaymentXPayRequest authRequest = OBJECT_MAPPER.readValue(json, AuthPaymentXPayRequest.class);

        BigInteger grandTotal = authRequest.getImporto();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String mac = createMac(codTrans, grandTotal, timeStamp);

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

    private AuthPaymentXPayRequest createXpayAuthRequest(XPayAuthRequest pgsRequest) {
        String idTransaction = pgsRequest.getIdTransaction();
        String codTrans = StringUtils.leftPad(idTransaction, 2, ZERO_CHAR);
        String timeStamp = String.valueOf(System.currentTimeMillis());
        BigInteger grandTotal = pgsRequest.getGrandTotal();
        String mac = createMac(codTrans, grandTotal, timeStamp);

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

    private String createMac(String codTrans, BigInteger importo, String timeStamp) {
        String macString = String.format("apiKey=%scodiceTransazione=%sdivisa=%simporto=%stimeStamp=%s%s",
                apiKey, codTrans, EUR_CURRENCY, importo, timeStamp, secretKey);
        return hashMac(macString);
    }

    private String hashMac(String macString) {
        String hash = StringUtils.EMPTY;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] in = digest.digest(macString.getBytes(StandardCharsets.UTF_8));

            final StringBuilder builder = new StringBuilder();
            for (byte b : in) {
                builder.append(String.format("%02x", b));
            }
            hash = builder.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("hashMac", e);
        }
        return hash;
    }

}
