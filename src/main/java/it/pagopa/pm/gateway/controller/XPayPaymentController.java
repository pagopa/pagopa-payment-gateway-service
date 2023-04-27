package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.xpay.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.XpayService;
import it.pagopa.pm.gateway.service.async.XPayPaymentAsyncService;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import it.pagopa.pm.gateway.utils.EcommercePatchUtils;
import it.pagopa.pm.gateway.utils.JwtTokenUtils;
import it.pagopa.pm.gateway.utils.XPayUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.Headers.X_CLIENT_ID;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.constant.XPayParams.*;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;
import static it.pagopa.pm.gateway.dto.xpay.EsitoXpay.OK;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
@NoArgsConstructor
@RequestMapping(REQUEST_PAYMENTS_XPAY)
public class XPayPaymentController {

    private static final String ECOMMERCE_APP_ORIGIN = "ECOMMERCE_APP";
    private static final String ECOMMERCE_WEB_ORIGIN = "ECOMMERCE_WEB";
    private static final String PGS_GENERIC_ERROR = "1000";
    private static final List<String> VALID_CLIENT_ID = Arrays.asList(ECOMMERCE_APP_ORIGIN, ECOMMERCE_WEB_ORIGIN);
    public static final String EUR_CURRENCY = "978";
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String ZERO_CHAR = "0";
    private String xpayPollingUrl;
    private String xpayResumeUrl;
    private ClientsConfig clientsConfig;
    private String apiKey;
    private PaymentRequestRepository paymentRequestRepository;
    private XpayService xpayService;
    private XPayUtils xPayUtils;
    private JwtTokenUtils jwtTokenUtils;
    private XPayPaymentAsyncService xPayPaymentAsyncService;
    private EcommercePatchUtils ecommercePatchUtils;

    @Autowired
    public XPayPaymentController(@Value("${xpay.polling.url}") String xpayPollingUrl, @Value("${xpay.resume.url}") String xpayResumeUrl,
                                 @Value("${xpay.apiKey}") String apiKey, PaymentRequestRepository paymentRequestRepository, XpayService xpayService,
                                 XPayUtils xPayUtils, JwtTokenUtils jwtTokenUtils, ClientsConfig clientsConfig,
                                 XPayPaymentAsyncService xPayPaymentAsyncService, EcommercePatchUtils ecommercePatchUtils) {
        this.xpayPollingUrl = xpayPollingUrl;
        this.xpayResumeUrl = xpayResumeUrl;
        this.apiKey = apiKey;
        this.paymentRequestRepository = paymentRequestRepository;
        this.xpayService = xpayService;
        this.xPayUtils = xPayUtils;
        this.jwtTokenUtils = jwtTokenUtils;
        this.clientsConfig = clientsConfig;
        this.xPayPaymentAsyncService = xPayPaymentAsyncService;
        this.ecommercePatchUtils = ecommercePatchUtils;
    }

    @PostMapping
    public ResponseEntity<XPayAuthResponse> requestPaymentsXPay(@RequestHeader(value = X_CLIENT_ID) String clientId,
                                                                @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                @Valid @RequestBody XPayAuthRequest pgsRequest) {
        if (!VALID_CLIENT_ID.contains(clientId)) {
            log.info("START - POST " + REQUEST_PAYMENTS_XPAY);
            log.error(String.format("Client id %s is not valid", clientId));
            return createXpayAuthResponse(BAD_REQUEST_MSG_CLIENT_ID, HttpStatus.BAD_REQUEST, null, null, null);
        }

        if (ObjectUtils.anyNull(pgsRequest) || pgsRequest.getGrandTotal().equals(BigInteger.ZERO)) {
            log.info("START POST - " + REQUEST_PAYMENTS_XPAY);
            log.error(BAD_REQUEST_MSG);
            return createXpayAuthResponse(BAD_REQUEST_MSG, HttpStatus.BAD_REQUEST, null, null, null);
        }

        String idTransaction = pgsRequest.getIdTransaction();
        log.info("START - POST {} for transactionId {}", REQUEST_PAYMENTS_XPAY, idTransaction);
        setMdcFields(mdcFields);

        if (Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createXpayAuthResponse(TRANSACTION_ALREADY_PROCESSED_MSG, HttpStatus.UNAUTHORIZED, null, null, idTransaction);
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
            XPayPollingResponse xPayPollingResponse = new XPayPollingResponse();
            xPayPollingResponse.setErrorDetail("requestId " + requestId + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(xPayPollingResponse);
        }
        return createXPayAuthPollingResponse(entity);
    }

    @GetMapping(REQUEST_PAYMENTS_RESUME)
    public ResponseEntity<String> resumeXPayPayment(@PathVariable String requestId,
                                                    @RequestParam Map<String, String> params) {

        log.info("START - GET {}{} for requestId {}", REQUEST_PAYMENTS_XPAY, REQUEST_PAYMENTS_RESUME, requestId);
        log.info("Params received from XPay{} for requestId: {}", params, requestId);

        XPay3DSResponse xPay3DSResponse = buildXPay3DSResponse(params);
        EsitoXpay outcome = xPay3DSResponse.getOutcome();

        String pollingUrlRedirect = StringUtils.join(xpayPollingUrl, requestId);
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
        if (Objects.isNull(entity)) {
            log.error("No XPay entity has been found for requestId: " + requestId);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(pollingUrlRedirect)).build();
        }

        if (outcome.equals(OK) && checkResumeRequest(entity, requestId, xPay3DSResponse)
                && CREATED.name().equals(entity.getStatus())) {

            xPayPaymentAsyncService.executeXPayPaymentCall(requestId, xPay3DSResponse, entity);
        } else {
            log.info(String.format("Outcome is %s: setting status as DENIED for requestId %s", outcome, requestId));
            entity.setStatus(DENIED.name());
            paymentRequestRepository.save(entity);

            ecommercePatchUtils.executePatchTransaction(entity);
        }

        log.info("END - GET {}{} for requestId {}", REQUEST_PAYMENTS_XPAY, REQUEST_PAYMENTS_RESUME, requestId);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(pollingUrlRedirect)).build();
    }

    private ResponseEntity<XPayAuthResponse> createXpayAuthResponse(String errorMessage, HttpStatus status, String requestId, String timeStamp, String transactionId) {
        XPayAuthResponse response = new XPayAuthResponse();
        response.setTimeStamp(timeStamp);
        if (StringUtils.isNotBlank(requestId)) {
            response.setRequestId(requestId);
        }

        if (StringUtils.isEmpty(errorMessage)) {
            String sessionToken = jwtTokenUtils.generateToken(requestId);
            String pollingUrlRedirect = xpayPollingUrl + requestId + "#token=" + sessionToken;
            response.setUrlRedirect(pollingUrlRedirect);
        } else {
            response.setError(errorMessage);
        }

        log.info("END - POST {} for transactionId {} and requestId {}", REQUEST_PAYMENTS_XPAY, transactionId, requestId);
        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping(REQUEST_ID)
    public ResponseEntity<XPayRefundResponse> refundXpayPayment(@PathVariable String requestId) {
        log.info("START - requesting XPay refund for requestId: " + requestId);
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);

        if (Objects.isNull(entity)) {
            log.error(REQUEST_ID_NOT_FOUND_MSG);
            return createXPayRefundResponse(requestId, HttpStatus.NOT_FOUND, null);
        }

        if (BooleanUtils.isTrue(entity.getIsRefunded())) {
            log.info("RequestId " + requestId + " has been refunded already. Skipping refund");
            return createXPayRefundResponse(requestId, HttpStatus.OK, entity);
        }

        try {
            EsitoXpay outcomeOrderStatus = executeXPayOrderStatus(entity);
            if (outcomeOrderStatus.equals(OK)) {
                executeXPayRevert(entity);
            }
            return createXPayRefundResponse(requestId, HttpStatus.OK, entity);
        } catch (Exception e) {
            return createXPayRefundResponse(requestId, HttpStatus.INTERNAL_SERVER_ERROR, entity);
        }
    }

    private ResponseEntity<XPayAuthResponse> createAuthPaymentXpay(XPayAuthRequest pgsRequest, String clientId, String mdcFields) {
        String transactionId = pgsRequest.getIdTransaction();
        log.info("START - requesting XPay payment authorization for transactionId " + transactionId);

        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        AuthPaymentXPayRequest xPayAuthRequest = createXpayAuthRequest(pgsRequest);
        generateRequestEntity(clientId, mdcFields, transactionId, paymentRequestEntity, xPayAuthRequest);

        String requestId = paymentRequestEntity.getGuid();
        log.info("Created request entity for transactionId {} with requestId {}", transactionId, requestId);

        xPayAuthRequest.setUrlRisposta(String.format(xpayResumeUrl, paymentRequestEntity.getGuid()));
        xPayPaymentAsyncService.executeXPayAuthorizationCall(xPayAuthRequest, paymentRequestEntity, transactionId);

        return createXpayAuthResponse(null, HttpStatus.ACCEPTED, requestId, paymentRequestEntity.getTimeStamp(), transactionId);
    }

    private ResponseEntity<XPayPollingResponse> createXPayAuthPollingResponse(PaymentRequestEntity paymentRequestEntity) {
        String requestId = paymentRequestEntity.getGuid();
        PaymentRequestStatusEnum statusEnum = getEnumValueFromString(paymentRequestEntity.getStatus());
        log.info("START - create XPay polling response for requestId {} - status {}", requestId, statusEnum);

        XPayPollingResponse response = new XPayPollingResponse();
        response.setRequestId(requestId);
        response.setPaymentRequestStatusEnum(statusEnum);
        if (statusEnum.equals(CREATED)) {
            response.setHtml(paymentRequestEntity.getXpayHtml());
        }
        OutcomeXpayGateway outcomeXpayGateway = buildOutcomeXpayGateway(paymentRequestEntity.getErrorCode(),
                paymentRequestEntity.getAuthorizationCode(), statusEnum);
        response.setOutcomeXpayGateway(outcomeXpayGateway);

        if (isStatusOneOf(statusEnum, AUTHORIZED, DENIED, CANCELLED)) {
            ClientConfig clientConfig = clientsConfig.getByKey(paymentRequestEntity.getClientId());
            String clientReturnUrl = clientConfig.getXpay().getClientReturnUrl();
            response.setRedirectUrl(StringUtils.join(clientReturnUrl, paymentRequestEntity.getIdTransaction()));
        }

        log.info("END - create XPay polling response for requestId {}", requestId);
        return ResponseEntity.ok().body(response);
    }

    private boolean isStatusOneOf(PaymentRequestStatusEnum referenceStatus, PaymentRequestStatusEnum option1,
                                  PaymentRequestStatusEnum option2, PaymentRequestStatusEnum... otherOptions) {
        Set<PaymentRequestStatusEnum> paymentRequestStatusEnums = Arrays.stream(otherOptions).collect(Collectors.toSet());
        paymentRequestStatusEnums.add(option1);
        paymentRequestStatusEnums.add(option2);
        return paymentRequestStatusEnums.contains(referenceStatus);
    }

    private static OutcomeXpayGateway buildOutcomeXpayGateway(String errorCode, String authorizationCode,
                                                              PaymentRequestStatusEnum paymentRequestStatusEnum) {
        OutcomeXpayGateway outcomeXpayGateway = new OutcomeXpayGateway();
        outcomeXpayGateway.setErrorCode(errorCode);
        switch (paymentRequestStatusEnum) {
            case AUTHORIZED:
                outcomeXpayGateway.setOutcomeEnum(OutcomeEnum.OK);
                outcomeXpayGateway.setAuthorizationCode(authorizationCode);
                break;
            case CANCELLED:
                outcomeXpayGateway.setOutcomeEnum(OutcomeEnum.OK);
                break;
            case DENIED:
                outcomeXpayGateway.setOutcomeEnum(OutcomeEnum.KO);
                break;
            default:
                break;
        }
        return outcomeXpayGateway;
    }

    private boolean checkResumeRequest(PaymentRequestEntity entity, String requestId, XPay3DSResponse xpay3DSResponse) {
        log.info("CheckResumeRequest for requestId: " + requestId);

        if (Objects.nonNull(entity.getAuthorizationOutcome())) {
            log.warn(String.format("requestId %s already processed", requestId));
            entity.setErrorMessage("requestId already processed");
            entity.setErrorCode(PGS_GENERIC_ERROR);
            return false;
        }

        String xPayMac = xpay3DSResponse.getMac();
        if (BooleanUtils.isFalse(xPayUtils.checkMac(xPayMac, xpay3DSResponse))) {
            log.error(String.format(MAC_NOT_EQUAL_ERROR_MSG, xPayMac) + "for requestId: " + requestId);
            entity.setErrorMessage("Mac not Equal");
            entity.setErrorCode(PGS_GENERIC_ERROR);
            return false;
        }

        return true;

    }

    private XPay3DSResponse buildXPay3DSResponse(Map<String, String> params) {
        log.debug("Building XPay3DSResponse");
        XPay3DSResponse xPay3DSResponse = new XPay3DSResponse();
        xPay3DSResponse.setOutcome(EsitoXpay.valueOf(params.get(XPAY_OUTCOME)));
        xPay3DSResponse.setOperationId(params.get(XPAY_OPERATION_ID));
        xPay3DSResponse.setTimestamp(params.get(XPAY_TIMESTAMP));
        xPay3DSResponse.setMac(params.get(XPAY_MAC));
        xPay3DSResponse.setXpayNonce(params.get(XPAY_NONCE));
        xPay3DSResponse.setErrorCode(params.get(XPAY_ERROR_CODE));
        xPay3DSResponse.setErrorMessage(params.get(XPAY_ERROR_MESSAGE));
        return xPay3DSResponse;
    }

    private EsitoXpay executeXPayOrderStatus(PaymentRequestEntity entity) {
        String requestId = entity.getGuid();
        XPayOrderStatusResponse response;
        try {
            log.info("Calling situazioneOrdine for requestId " + requestId);
            XPayOrderStatusRequest request = createXPayOrderStatusRequest(entity);
            response = xpayService.callSituazioneOrdine(request);
            return response.getEsito();
        } catch (Exception e) {
            log.error("Error while calling XPay's situazioneOrdine API for requestId " + requestId, e);
            throw e;
        }
    }

    private void executeXPayRevert(PaymentRequestEntity entity) throws Exception {
        String requestId = entity.getGuid();
        try {
            log.info("Calling revert API for requestId " + requestId);
            XPayRevertRequest request = createXPayRevertRequest(entity);
            XPayRevertResponse response = xpayService.callStorna(request);
            EsitoXpay outcome = response.getEsito();
            log.info(String.format("XPay response to revert API is %s for requestId %s", outcome, requestId));
            if (outcome.equals(OK)) {
                entity.setStatus(CANCELLED.name());
                entity.setIsRefunded(true);
                paymentRequestRepository.save(entity);
            }
        } catch (Exception e) {
            log.error(GENERIC_REFUND_ERROR_MSG + requestId, e);
            throw e;
        }
    }

    private ResponseEntity<XPayRefundResponse> createXPayRefundResponse(String requestId, HttpStatus httpStatus, PaymentRequestEntity entity) {
        XPayRefundResponse response = new XPayRefundResponse();
        response.setRequestId(requestId);

        if (entity != null)
            response.setStatus(entity.getStatus());

        if (httpStatus.is4xxClientError()) {
            response.setError(REQUEST_ID_NOT_FOUND_MSG);
        } else if (!httpStatus.is2xxSuccessful()) {
            response.setError(GENERIC_REFUND_ERROR_MSG + requestId);
        }

        log.info("END - requesting XPay refund for requestId: " + requestId);
        return ResponseEntity.status(httpStatus).body(response);
    }

    private XPayOrderStatusRequest createXPayOrderStatusRequest(PaymentRequestEntity entity) {
        String idTransaction = entity.getIdTransaction();
        String codTrans = StringUtils.leftPad(idTransaction, 2, ZERO_CHAR);

        String timeStamp = String.valueOf(System.currentTimeMillis());
        String mac = xPayUtils.createMacForOrderStatus(codTrans, timeStamp);

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
        String mac = xPayUtils.createMac(codTrans, grandTotal, timeStamp);

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
        XpayPersistableRequest xpayPersistableRequest = generateXpayDatabaseRequest(request);
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_XPAY);
        paymentRequestEntity.setIdTransaction(transactionId);
        paymentRequestEntity.setMdcInfo(mdcFields);
        paymentRequestEntity.setTimeStamp(request.getTimeStamp());
        paymentRequestEntity.setStatus(CREATED.name());
        try {
            paymentRequestEntity.setJsonRequest(xpayPersistableRequest);
        } catch (JsonProcessingException e) {
            log.error("Error while serializing request as JSON. Request object is: {}", request, e);
        }
    }

    private XpayPersistableRequest generateXpayDatabaseRequest(AuthPaymentXPayRequest request) {
        return new XpayPersistableRequest(request.getCodiceTransazione(),
                request.getImporto(), request.getDivisa(), request.getTimeStamp());
    }

}