package it.pagopa.pm.gateway.service.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.xpay.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestLockRepository;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.XpayService;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import it.pagopa.pm.gateway.utils.EcommercePatchUtils;
import it.pagopa.pm.gateway.utils.XPayUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Objects;

import static it.pagopa.pm.gateway.constant.Messages.GENERIC_ERROR_MSG;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.AUTHORIZED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.DENIED;
import static it.pagopa.pm.gateway.dto.xpay.EsitoXpay.KO;
import static it.pagopa.pm.gateway.dto.xpay.EsitoXpay.OK;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@Service
@Slf4j
@NoArgsConstructor
public class XPayPaymentAsyncService {
    private static final int MAX_RETRIES = 3;
    public static final String EUR_CURRENCY = "978";
    public static final String ZERO_CHAR = "0";
    private static final int MAX_ERROR_MESSAGE_ENTITY_SIZE = 50;
    private static final String ERROR_MESSAGE_TRUNCATE_SUFFIX = "...";
    private static final String PGS_GENERIC_ERROR = "1000";
    private XpayService xpayService;
    private PaymentRequestRepository paymentRequestRepository;
    private PaymentRequestLockRepository paymentRequestLockRepository;
    private XPayUtils xPayUtils;
    private String apiKey;
    private ClientsConfig clientsConfig;
    private EcommerceClient ecommerceClient;
    private EcommercePatchUtils ecommercePatchUtils;

    @Autowired
    public XPayPaymentAsyncService(PaymentRequestRepository paymentRequestRepository, XpayService xpayService, XPayUtils xPayUtils,
                                   @Value("${xpay.apiKey}") String apiKey, ClientsConfig clientsConfig, EcommerceClient ecommerceClient,
                                   EcommercePatchUtils ecommercePatchUtils, PaymentRequestLockRepository paymentRequestLockRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.xpayService = xpayService;
        this.apiKey = apiKey;
        this.ecommerceClient = ecommerceClient;
        this.xPayUtils = xPayUtils;
        this.clientsConfig = clientsConfig;
        this.ecommercePatchUtils = ecommercePatchUtils;
        this.paymentRequestLockRepository = paymentRequestLockRepository;
    }

    @Async
    public void executeXPayAuthorizationCall(AuthPaymentXPayRequest xPayRequest, PaymentRequestEntity requestEntity, String transactionId) {
        String requestId = requestEntity.getGuid();
        setMdcFields(requestEntity.getMdcInfo());
        log.info("START - execute XPay payment authorization call for transactionId {} and requestId {} ", transactionId, requestId);
        try {
            AuthPaymentXPayResponse response = xpayService.callAutenticazione3DS(xPayRequest);
            if (ObjectUtils.isEmpty(response)) {
                String errorMsg = "Response from XPay to /autenticazione3DS is empty";
                log.error(errorMsg);
                requestEntity.setStatus(DENIED.name());
            } else {
                requestEntity.setTimeStamp(xPayRequest.getTimeStamp());
                XpayError xpayError = response.getErrore();
                if (ObjectUtils.isEmpty(xpayError)) {
                    log.info("autenticazione3DS for requestId {} returns no error", requestId);
                    requestEntity.setXpayHtml(response.getHtml());
                    requestEntity.setCorrelationId(response.getIdOperazione());
                } else {
                    log.info("autenticazione3DS for requestId {} returns errors", requestId);
                    setErrorCodeAndMessage(requestEntity.getGuid(), requestEntity, xpayError);
                    requestEntity.setStatus(DENIED.name());
                }

                if (DENIED.name().equals(requestEntity.getStatus())) {
                    ecommercePatchUtils.executePatchTransactionXPay(requestEntity);
                }

                paymentRequestRepository.save(requestEntity);
                log.info("END - XPay Request Payment Authorization for transactionId {} and requestId {} ", transactionId, requestId);
            }
        } catch (Exception e) {
            log.error("{}{} and requestId: {}", GENERIC_ERROR_MSG, transactionId, requestId, e);
        }
    }

    //@Async
    public void executeXPayPaymentCall(String requestId, XPay3DSResponse xpay3DSResponse, PaymentRequestEntity entity) {
        setMdcFields(entity.getMdcInfo());
        log.info("START - executeXPayPaymentCall for requestId " + requestId);
        String xpayNonce = xpay3DSResponse.getXpayNonce();
        entity.setXpayNonce(xpayNonce);
        int retryCount = 1;
        boolean isAuthorized = false;
        log.info("Calling XPay /paga3DS - requestId: " + requestId);
        while (!isAuthorized && retryCount <= MAX_RETRIES) {
            try {
                PaymentXPayRequest xpayRequest = createXPayPaymentRequest(requestId, entity, xpayNonce);
                log.info(String.format("Attempt no.%s for requestId: %s", retryCount, requestId));
                PaymentXPayResponse response = xpayService.callPaga3DS(xpayRequest);
                if (ObjectUtils.isEmpty(response)) {
                    log.warn(String.format("paga3DS response from XPay to requestId %s is empty", requestId));
                    retryCount++;
                    entity.setStatus(DENIED.name());
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
                        setErrorCodeAndMessage(requestId, entity, response.getErrore());
                        retryCount++;
                    }
                }
            } catch (Exception e) {
                log.error("An exception occurred while calling XPay's /paga3DS for requestId: {}. Cause: {}, message: {}", requestId, e.getCause(), e.getMessage(), e);
                retryCount++;
                entity.setStatus(DENIED.name());
            }
        }
        entity.setTimeStamp(xpay3DSResponse.getTimestamp());
        entity.setAuthorizationOutcome(isAuthorized);
        paymentRequestRepository.save(entity);

        ecommercePatchUtils.executePatchTransactionXPay(entity);

        log.info(String.format("END - executeXPayPaymentCall for requestId: %s. Status: %s " +
                "- Authorization: %s. Retry attempts number: %s", requestId, entity.getStatus(), isAuthorized, retryCount));
    }

    private PaymentXPayRequest createXPayPaymentRequest(String requestId, PaymentRequestEntity entity, String xpayNonce) throws JsonProcessingException {
        String idTransaction = entity.getIdTransaction();
        String codTrans = StringUtils.leftPad(idTransaction, 2, ZERO_CHAR);

        BigInteger grandTotal = xPayUtils.getGrandTotalForMac(entity);
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String mac = xPayUtils.createPaymentMac(codTrans, grandTotal, timeStamp, xpayNonce);

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


    private void setErrorCodeAndMessage(String requestId, PaymentRequestEntity entity, XpayError xpayError) {
        if (ObjectUtils.isNotEmpty(xpayError)) {
            String errorCode = String.valueOf(xpayError.getCodice());
            String errorMessage = xpayError.getMessaggio();
            log.info("RequestId {} has error code: {} - message: {}", requestId,
                    errorCode, errorMessage);
            String truncatedMessage = errorMessage;
            if (StringUtils.length(truncatedMessage) > MAX_ERROR_MESSAGE_ENTITY_SIZE) {
                truncatedMessage = StringUtils.truncate(errorMessage, MAX_ERROR_MESSAGE_ENTITY_SIZE - ERROR_MESSAGE_TRUNCATE_SUFFIX.length()) + ERROR_MESSAGE_TRUNCATE_SUFFIX;
            }
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(truncatedMessage);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareResume(String requestId, XPay3DSResponse xpay3DSResponse) {
        log.info("prepareResume for requestId: " + requestId);

        PaymentRequestEntity entity = paymentRequestLockRepository.findByGuid(requestId);

        if (Objects.isNull(entity)) {
            log.error("No XPay entity has been found for requestId: {}", requestId);
            return false;
        }

        if (PaymentRequestStatusEnum.CREATED.name().equals(entity.getStatus())) {
            log.info("prepareResume request in state CREATED - proceed for requestId: {}", requestId);
            entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
            paymentRequestLockRepository.save(entity);
            return true;
        } else {
            log.info("prepareResume request in state {} - not proceed for requestId: {}", entity.getStatus(), requestId);
        }
        return false;
    }
}