package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.XPayAuthRequest;
import it.pagopa.pm.gateway.dto.XPayAuthResponse;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayRequest;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.entity.PaymentResponseEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.repository.PaymentResponseRepository;
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
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_XPAY;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.Headers.X_CLIENT_ID;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CREATED;
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

    @Value("${pgs.xpay.response.urlredirect}")
    private String PGS_RESPONSE_URL_REDIRECT;

    @Value("${pgs.xpay.request.responseUrl}")
    private String XPAY_RESPONSE_URL;

    @Value("${pgs.xpay.apiKey}")
    private String API_KEY;

    @Value("${pgs.xpay.secretKey}")
    private String SECRET_KEY;

    @Value("${pgs.xpay.authenticationUrl}")
    private String XPAY_AUTH_URL;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private RestTemplate xpayRestTemplate;

    @Autowired
    private PaymentResponseRepository paymentResponseRepository;

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

    private ResponseEntity<XPayAuthResponse> createxPayAuthResponse(String errorMessage, HttpStatus status, String requestId) {
        XPayAuthResponse respone = new XPayAuthResponse();
        respone.setRequestId(requestId);
        if(StringUtils.isEmpty(errorMessage)) {
            String urlRedirect = String.format(PGS_RESPONSE_URL_REDIRECT, requestId);
            respone.setUrlRedirect(urlRedirect);
        } else {
            respone.setError(errorMessage);
            log.info("END POST - " + REQUEST_PAYMENTS_XPAY);
        }
        return ResponseEntity.status(status).body(respone);
    }

    private ResponseEntity<XPayAuthResponse> createAuthPaymentXPay(XPayAuthRequest pgsRequest, String clientId, String mdcFields) {
       log.info("START - XPay Request Payment Authorization ");
        AuthPaymentXPayRequest xPayRequest = createXPayRequest(pgsRequest);

        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        String transactionId = pgsRequest.getIdTransaction();
        try{
            String authRequestJson = OBJECT_MAPPER.writeValueAsString(xPayRequest);
            generateRequestEntity(clientId, mdcFields, transactionId, paymentRequestEntity, xPayRequest.getTimeStamp());
            paymentRequestEntity.setJsonRequest(authRequestJson);
        } catch (JsonProcessingException e) {
            log.error(SERIALIZATION_ERROR_MSG, e);
            return  createxPayAuthResponse(GENERIC_ERROR_MSG + transactionId, HttpStatus.INTERNAL_SERVER_ERROR, null);
        }

        try {
            executeXPayAuthorizationCall(xPayRequest, paymentRequestEntity, transactionId);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG, e);
            return createxPayAuthResponse(GENERIC_ERROR_MSG + transactionId, HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
        log.info("END - XPay Request Payment Authorization ");
        return createxPayAuthResponse(null, HttpStatus.OK, paymentRequestEntity.getGuid());
    }

    @Async
    private void executeXPayAuthorizationCall(AuthPaymentXPayRequest xPayRequest, PaymentRequestEntity paymentRequestEntity, String transactionId) {
        log.info("START - execute XPay payment authorization call for transactionId: " + transactionId);
        AuthPaymentXPayResponse response;
        PaymentResponseEntity paymentResponseEntity = new PaymentResponseEntity();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AuthPaymentXPayRequest> entity = new HttpEntity<>(xPayRequest, headers);
            response = xpayRestTemplate.postForObject(XPAY_AUTH_URL, entity, AuthPaymentXPayResponse.class);
        } catch (ResourceAccessException e) {
            log.warn("Timeout exception for xpay api request call");
            throw e;
        } catch (HttpServerErrorException hse){
            log.warn("HttpServerErrorException exception for xpay api request call");
            throw hse;
        }

        paymentRequestEntity.setXpayHtml(response.getHtml());
        paymentRequestEntity.setTimeStamp(String.valueOf(response.getTimeStamp()));

        generateResponseEntity(response, paymentResponseEntity, paymentRequestEntity.getGuid());
        paymentResponseRepository.save(paymentResponseEntity);

        paymentRequestRepository.save(paymentRequestEntity);
        log.info("END - execute XPay payment authorization call for transactionId: " + transactionId);
    }

    private AuthPaymentXPayRequest createXPayRequest(XPayAuthRequest pgsRequest) {
        log.info("create Request body to call XPay");
        String codTrans = retrieveCodTrans(pgsRequest.getIdTransaction());
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String mac = createMac(codTrans, pgsRequest.getGrandTotal(), timeStamp);
        AuthPaymentXPayRequest xPayRequest = new AuthPaymentXPayRequest();
        xPayRequest.setApiKey(API_KEY);
        xPayRequest.setImporto(pgsRequest.getGrandTotal());
        xPayRequest.setCvv(pgsRequest.getCvv());
        xPayRequest.setPan(pgsRequest.getPan());
        xPayRequest.setDivisa(EUR_CURRENCY);
        xPayRequest.setMac(mac);
        xPayRequest.setScadenza(pgsRequest.getExpiryDate());
        xPayRequest.setTimeStamp(timeStamp);
        xPayRequest.setCodiceTransazione(codTrans);
        xPayRequest.setUrlRisposta(XPAY_RESPONSE_URL);
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

    private void generateResponseEntity(AuthPaymentXPayResponse response, PaymentResponseEntity paymentResponseEntity, String requestId) {
        paymentResponseEntity.setRequestId(requestId);
        paymentResponseEntity.setMac(response.getMac());
        paymentResponseEntity.setTimeStamp(String.valueOf(response.getTimeStamp()));
        paymentResponseEntity.setHtml(response.getHtml());
        paymentResponseEntity.setOperationId(response.getIdOperazione());
        paymentResponseEntity.setErrorCode(paymentResponseEntity.getErrorCode());
        paymentResponseEntity.setErrorMessage(paymentResponseEntity.getErrorMessage());
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
