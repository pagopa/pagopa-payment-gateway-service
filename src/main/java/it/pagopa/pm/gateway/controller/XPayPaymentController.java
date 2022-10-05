package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.XPayAuthPollingResponse;
import it.pagopa.pm.gateway.dto.XPayAuthRequest;
import it.pagopa.pm.gateway.dto.XPayAuthResponse;
import it.pagopa.pm.gateway.dto.XPayPollingResponseError;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayRequest;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.XpayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigInteger;
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
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CREATED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.DENIED;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;
import static java.lang.String.format;

@RestController
@Slf4j
@RequestMapping(REQUEST_PAYMENTS_XPAY)
public class XPayPaymentController {

    private static final String APP_ORIGIN = "APP";
    private static final String WEB_ORIGIN = "WEB";
    private static final List<String> VALID_CLIENT_ID = Arrays.asList(APP_ORIGIN, WEB_ORIGIN);
    private static final String EUR_CURRENCY = "978";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${xpay.response.urlredirect}")
    private String PGS_RESPONSE_URL_REDIRECT;

    @Value("${xpay.request.responseUrl}")
    private String XPAY_RESPONSE_URL;

    @Value("${xpay.apiKey}")
    private String API_KEY;

    @Value("${xpay.secretKey}")
    private String SECRET_KEY;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private XpayService service;

    @PostMapping()
    public ResponseEntity<XPayAuthResponse> requestPaymentsXPay(@RequestHeader(value = X_CLIENT_ID) String clientId,
                                                                @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                @RequestBody XPayAuthRequest pgsRequest) {

        log.info("START POST - " + REQUEST_PAYMENTS_XPAY);
        setMdcFields(mdcFields);
        String idTransaction = pgsRequest.getIdTransaction();

        if (ObjectUtils.anyNull(pgsRequest) || pgsRequest.getGrandTotal().equals(BigInteger.ZERO)) {
            log.error("Bad Request - " + BAD_REQUEST_MSG);
            return createxPayAuthResponse(BAD_REQUEST_MSG, HttpStatus.BAD_REQUEST, null);
        }

        if (!VALID_CLIENT_ID.contains(clientId)) {
            log.error(format("Client id %s is not valid", clientId));
            return createxPayAuthResponse(BAD_REQUEST_MSG_CLIENT_ID, HttpStatus.BAD_REQUEST, null);
        }

        if(Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createxPayAuthResponse(TRANSACTION_ALREADY_PROCESSED_MSG, HttpStatus.UNAUTHORIZED, null);
        }
        log.info("END POST - " + REQUEST_PAYMENTS_XPAY);
        return createAuthPaymentXPay(pgsRequest, clientId, mdcFields);
    }

    @GetMapping(XPAY_AUTH)
    public ResponseEntity<XPayAuthPollingResponse> getXPayAuthorizationResponse(@PathVariable String requestId,
                                                                @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) {
        log.info(("SART - get XPay Authorization response for requestId: " + requestId));
        setMdcFields(mdcFields);

        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
        if(Objects.isNull(entity) || !entity.getRequestEndpoint().equals(REQUEST_PAYMENTS_XPAY)){
            log.error("No xPay request entity has been found for requestId: " + requestId);
            XPayPollingResponseError error = new XPayPollingResponseError(404L, NOT_FOUND_MSG);
            return createXPayAuthPollingResponse(HttpStatus.NOT_FOUND, error, null);
        }
        return  createXPayAuthPollingResponse(HttpStatus.OK, null, entity);
    }

    private ResponseEntity<XPayAuthResponse> createxPayAuthResponse(String errorMessage, HttpStatus status, String requestId) {
        XPayAuthResponse response = new XPayAuthResponse();
        if(Objects.nonNull(requestId)) {
            PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
            response.setRequestId(requestId);
            response.setStatus(entity.getStatus());
        }
        if(StringUtils.isEmpty(errorMessage)) {
            String urlRedirect = StringUtils.join(PGS_RESPONSE_URL_REDIRECT, requestId);
            response.setUrlRedirect(urlRedirect);
        } else {
            response.setError(errorMessage);
            log.info("END POST - " + REQUEST_PAYMENTS_XPAY);
        }
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<XPayAuthResponse> createAuthPaymentXPay(XPayAuthRequest pgsRequest, String clientId, String mdcFields) {
       log.info("START - XPay Request Payment Authorization ");

        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        String transactionId = pgsRequest.getIdTransaction();
        try{
            AuthPaymentXPayRequest xPayRequest = createXPayRequest(pgsRequest);
            String authRequestJson = OBJECT_MAPPER.writeValueAsString(xPayRequest);
            generateRequestEntity(clientId, mdcFields, transactionId, paymentRequestEntity, xPayRequest.getTimeStamp());
            paymentRequestEntity.setJsonRequest(authRequestJson);
            if (Objects.nonNull(XPAY_RESPONSE_URL)) {
                xPayRequest.setUrlRisposta(String.format(XPAY_RESPONSE_URL, paymentRequestEntity.getGuid()));
            }
            else {
                log.warn("xPayResponseUrl is null");
                throw new Exception();
            }

            executeXPayAuthorizationCall(xPayRequest, paymentRequestEntity, transactionId);

        } catch (JsonProcessingException e) {
            log.error(SERIALIZATION_ERROR_MSG, e);
            return  createxPayAuthResponse(GENERIC_ERROR_MSG + transactionId, HttpStatus.INTERNAL_SERVER_ERROR, null);
        } catch (Exception e ) {
            log.error(GENERIC_ERROR_MSG + transactionId);
            return createxPayAuthResponse(GENERIC_ERROR_MSG + transactionId, HttpStatus.INTERNAL_SERVER_ERROR, null);
        }

        log.info("END - XPay Request Payment Authorization ");
        return createxPayAuthResponse(null, HttpStatus.OK, paymentRequestEntity.getGuid());
    }

    @Async
    private void executeXPayAuthorizationCall(AuthPaymentXPayRequest xPayRequest, PaymentRequestEntity paymentRequestEntity, String transactionId) throws Exception {
        log.info("START - execute XPay payment authorization call for transactionId: " + transactionId);
        AuthPaymentXPayResponse response;
        try {
            response = service.postForObject(xPayRequest);
            if(Objects.nonNull(response)) {
                paymentRequestEntity.setTimeStamp(String.valueOf(response.getTimeStamp()));
                if(Objects.isNull(response.getErrore())) {
                    paymentRequestEntity.setXpayHtml(response.getHtml());
                    paymentRequestEntity.setAuthorizationOutcome(true);
                } else {
                    paymentRequestEntity.setErrorCode(String.valueOf(response.getErrore().getCodice()));
                    paymentRequestEntity.setErrorMessage(response.getErrore().getMessaggio());
                    paymentRequestEntity.setStatus(DENIED.name());
                }
            } else {
                log.warn("Response received from xPay is null");
                throw new Exception();
            }
        } catch (ResourceAccessException e) {
            log.warn("Timeout exception for xpay api request call");
            throw e;
        } catch (HttpServerErrorException hse){
            log.warn("HttpServerErrorException exception for xpay api request call");
            throw hse;
        }

        paymentRequestRepository.save(paymentRequestEntity);
        log.info("END - execute XPay payment authorization call for transactionId: " + transactionId);
    }

    private ResponseEntity<XPayAuthPollingResponse> createXPayAuthPollingResponse(HttpStatus httpStatus, XPayPollingResponseError error, PaymentRequestEntity entity) {
        log.info("START - createXPayAuthPollingResponse");
        XPayAuthPollingResponse response = new XPayAuthPollingResponse();

        if (Objects.nonNull(error)) {
            response.setError(error);
            return ResponseEntity.status(httpStatus).body(response);
        }

        response.setStatus(entity.getStatus());

        if(Objects.nonNull(entity.getErrorCode()) && Objects.nonNull(entity.getErrorMessage())) {
            response.setError(new XPayPollingResponseError(Long.valueOf(entity.getErrorCode()), entity.getErrorMessage()));
            return ResponseEntity.ok().body(response);
        }

        if(Objects.nonNull(entity.getXpayHtml())) {
            response.setHtml(entity.getXpayHtml());
        }

        log.info("END - createXPayAuthPollingResponse");
        return ResponseEntity.ok().body(response);
    }

    private AuthPaymentXPayRequest createXPayRequest(XPayAuthRequest pgsRequest) throws Exception {
        log.info("create Request body to call XPay");
        String codTrans = retrieveCodTrans(pgsRequest.getIdTransaction());
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String mac = createMac(codTrans, pgsRequest.getGrandTotal(), timeStamp);
        AuthPaymentXPayRequest xPayRequest = new AuthPaymentXPayRequest();
        if(Objects.nonNull(API_KEY)) {
            xPayRequest.setApiKey(API_KEY);
        } else {
            throw new Exception();
        }
        xPayRequest.setImporto(pgsRequest.getGrandTotal());
        xPayRequest.setCvv(pgsRequest.getCvv());
        xPayRequest.setPan(pgsRequest.getPan());
        xPayRequest.setDivisa(EUR_CURRENCY);
        xPayRequest.setMac(mac);
        xPayRequest.setScadenza(pgsRequest.getExpiryDate());
        xPayRequest.setTimeStamp(timeStamp);
        xPayRequest.setCodiceTransazione(codTrans);
        return xPayRequest;
    }

    private void generateRequestEntity(String clientId, String mdcFields, String transactionId, PaymentRequestEntity paymentRequestEntity, String timeStamp) {
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_XPAY);
        paymentRequestEntity.setIdTransaction(transactionId);
        paymentRequestEntity.setMdcInfo(mdcFields);
        paymentRequestEntity.setTimeStamp(timeStamp);
        paymentRequestEntity.setStatus(CREATED.name());
    }

    private String retrieveCodTrans(String transactionId) {
        return StringUtils.leftPad(transactionId, 2, "0");
    }

    private String createMac(String codTrans, BigInteger importo, String timeStamp) {
        String macString = String.format("apiKey=%scodiceTransazione=%sdivisa=%simporto=%stimeStamp=%s%s",
                API_KEY, codTrans, EUR_CURRENCY, importo, timeStamp, SECRET_KEY);
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
