package it.pagopa.pm.gateway.controller;

import feign.FeignException;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.entity.BPayPaymentResponseEntity;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import it.pagopa.pm.gateway.repository.BPayPaymentResponseRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.lang.Exception;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.Headers.X_CORRELATION_ID;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.OK;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.*;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
public class BancomatPayPaymentTransactionsController {

    private static final String INQUIRY_RESPONSE_EFF = "EFF";
    @Autowired
    private BancomatPayClient bancomatPayClient;

    @Autowired
    private BPayPaymentResponseRepository bPayPaymentResponseRepository;

    @Autowired
    private RestapiCdClientImpl restapiCdClient;
    
    @PutMapping(REQUEST_PAYMENTS_BPAY)
    public ACKMessage updateTransaction(@RequestBody AuthMessage authMessage, @RequestHeader(X_CORRELATION_ID) String correlationId) throws RestApiException {
        MDC.clear();
        log.info("START Update transaction request for correlation-id: " + correlationId + ": " + authMessage);
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByCorrelationId(correlationId);
        if (alreadySaved == null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        } else {
            setMdcFields(alreadySaved.getMdcInfo());
            if (Boolean.TRUE.equals(alreadySaved.getIsProcessed())) {
                throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
            }
        }
        TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(authMessage.getAuthOutcome().equals(OK) ? TX_AUTHORIZED_BY_PGS.getId() : TX_REFUSED.getId(), authMessage.getAuthCode(), null, alreadySaved.getErrorCode());
        try {
            restapiCdClient.callTransactionUpdate(alreadySaved.getIdPagoPa(), transactionUpdate);
            alreadySaved.setIsProcessed(true);
            bPayPaymentResponseRepository.save(alreadySaved);
            return new ACKMessage(OK);
        } catch (FeignException fe) {
            log.error("Exception calling RestapiCD to update transaction", fe);
            throw new RestApiException(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR, fe.status());
        } catch (Exception e) {
            log.error("Exception updating transaction", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } finally {
            log.info("END Update transaction request for correlation-id: " + correlationId);
        }
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENTS_BPAY)
    public BPayPaymentOutcomeResponse requestPaymentToBancomatPay(@RequestBody BPayPaymentRequest request, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws Exception {
        setMdcFields(mdcFields);
        Long idPagoPa = request.getIdPagoPa();
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (alreadySaved != null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
        }
        log.info("START requestPaymentToBancomatPay " + idPagoPa);
        executePaymentRequest(request, mdcFields);
        log.info("END requestPaymentToBancomatPay " + idPagoPa);
        return new BPayPaymentOutcomeResponse(true);
    }

    @Transactional
    @PostMapping(REQUEST_REFUNDS_BPAY)
    public BPayRefundOutcomeResponse requestRefundToBancomatPay(@RequestBody BPayRefundRequest request, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws Exception {
        setMdcFields(mdcFields);
        Long idPagoPa = request.getIdPagoPa();
        log.info("START requestRefundToBancomatPay " + idPagoPa);
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (Objects.isNull(alreadySaved)) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        }
        String inquiryResponse = inquiryTransactionToBancomatPay(request);
        log.info("Inquiry response for idPagopa " + idPagoPa + ": " + inquiryResponse);
        boolean needsRefund = INQUIRY_RESPONSE_EFF.equals(inquiryResponse);
        boolean refunded = false;
        if (needsRefund) {
            refunded = executeRefundRequest(request);
        }
        return new BPayRefundOutcomeResponse(needsRefund, refunded);
    }

    private String inquiryTransactionToBancomatPay(BPayRefundRequest request) throws Exception {
        InquiryTransactionStatusResponse inquiryTransactionStatusResponse;
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();

        log.info("START requestInquiryTransactionToBancomatPay " + idPagoPa);

        inquiryTransactionStatusResponse = bancomatPayClient.sendInquiryRequest(request, guid);
        if (inquiryTransactionStatusResponse == null || inquiryTransactionStatusResponse.getReturn() == null) {
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }

        log.info("END requestInquiryTransactionToBancomatPay " + idPagoPa);
        return inquiryTransactionStatusResponse.getReturn().getEsitoPagamento();

    }

    public boolean executeRefundRequest(BPayRefundRequest request) throws RestApiException {
        boolean refunded = false;
        StornoPagamentoResponse response;
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();
        log.info("START executeRefundRequest for transaction " + idPagoPa + " with guid: " + guid);
        try {
            response = bancomatPayClient.sendRefundRequest(request, guid);
            EsitoVO esitoVO = response.getReturn().getEsito();
            log.info("Response from BPay sendRefundRequest - idPagopa: " + idPagoPa + " - esito: " + esitoVO.getCodice() + " - messaggio: " + esitoVO.getMessaggio());
            if (Boolean.TRUE.equals(esitoVO.isEsito())) {
                refunded = true;
            }
        } catch (Exception e) {
            log.error("Exception calling BancomatPay with idPagopa: " + idPagoPa, e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        log.info("END executeRefundRequest " + idPagoPa);
        return refunded;
    }

    @Async
    public void executePaymentRequest(BPayPaymentRequest request, String mdcInfo) throws RestApiException {
        InserimentoRichiestaPagamentoPagoPaResponse response;
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();
        log.info("START executePaymentRequest for transaction " + idPagoPa + " with guid: " + guid);
        try {
            response = bancomatPayClient.sendPaymentRequest(request, guid);
            if (response == null || response.getReturn() == null || response.getReturn().getEsito() == null) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            EsitoVO esitoVO = response.getReturn().getEsito();
            log.info("Response from BPay sendPaymentRequest - idPagopa: " + idPagoPa + " - correlationId: " + response.getReturn().getCorrelationId() + " - esito: " + esitoVO.getCodice() + " - messaggio: " + esitoVO.getMessaggio());
        } catch (Exception e) {
            log.error("Exception calling BancomatPay with idPagopa: " + idPagoPa, e);
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new RestApiException(ExceptionsEnum.TIMEOUT);
            }
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        BPayPaymentResponseEntity bPayPaymentResponseEntity = convertBpayPaymentResponseToEntity(response, idPagoPa, guid, mdcInfo);
        bPayPaymentResponseRepository.save(bPayPaymentResponseEntity);
        try {
            TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(TX_PROCESSING.getId(), null, null, bPayPaymentResponseEntity.getErrorCode());
            restapiCdClient.callTransactionUpdate(idPagoPa, transactionUpdate);
        } catch (FeignException e) {
            log.error("Exception calling RestapiCD transaction update", e);
            throw new RestApiException(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR, e.status());
        }
        log.info("END executePaymentRequest for transaction " + idPagoPa);
    }

    private BPayPaymentResponseEntity convertBpayPaymentResponseToEntity(InserimentoRichiestaPagamentoPagoPaResponse response, Long idPagoPa, String guid, String mdcInfo) {
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseReturnVO = response.getReturn();
        EsitoVO esitoVO = responseReturnVO.getEsito();
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        bPayPaymentResponseEntity.setOutcome(esitoVO.isEsito());
        bPayPaymentResponseEntity.setMessage(esitoVO.getMessaggio());
        bPayPaymentResponseEntity.setErrorCode(esitoVO.getCodice());
        bPayPaymentResponseEntity.setCorrelationId(responseReturnVO.getCorrelationId());
        bPayPaymentResponseEntity.setClientGuid(guid);
        bPayPaymentResponseEntity.setMdcInfo(mdcInfo);
        return bPayPaymentResponseEntity;
    }

}
