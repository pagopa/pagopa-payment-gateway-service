package it.pagopa.pm.gateway.controller;

import feign.*;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.client.restapicd.*;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.enums.*;
import it.pagopa.pm.gateway.entity.BPayPaymentResponseEntity;
import it.pagopa.pm.gateway.exception.*;
import it.pagopa.pm.gateway.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;

import java.lang.Exception;
import java.net.*;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Headers.*;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.*;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
public class PaymentTransactionsController {

    @Autowired
    private BancomatPayClient client;

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
        TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(authMessage.getAuthOutcome().equals(OutcomeEnum.OK) ? TX_AUTHORIZED_BANCOMAT_PAY.getId() : TX_REFUSED.getId(), authMessage.getAuthCode(), null);
        try {
            restapiCdClient.callTransactionUpdate(alreadySaved.getIdPagoPa(), transactionUpdate);
            alreadySaved.setIsProcessed(true);
            bPayPaymentResponseRepository.save(alreadySaved);
            return new ACKMessage(OutcomeEnum.OK);
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
    public BPayPaymentResponseEntity requestPaymentToBancomatPay(@RequestBody BPayPaymentRequest request, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws Exception {
        setMdcFields(mdcFields);
        Long idPagoPa = request.getIdPagoPa();
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (alreadySaved != null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
        }
        log.info("START requestPaymentToBancomatPay " + idPagoPa);
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setOutcome(true);
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        executePaymentRequest(request, mdcFields);
        log.info("END requestPaymentToBancomatPay " + idPagoPa);
        return bPayPaymentResponseEntity;
    }

    @Transactional
    @PostMapping(REQUEST_REFUNDS_BPAY)
    public void requestRefundToBancomatPay(@RequestBody BPayRefundRequest request, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws Exception {
        setMdcFields(mdcFields);
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();

        log.info("START requestRefundToBancomatPay " + idPagoPa);
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (alreadySaved == null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        }
        String inquiryResponse = inquiryTransactionToBancomatPay(request, guid);
        log.info("Inquiry response for idPagopa " + idPagoPa + ": " + inquiryResponse);
        switch (inquiryResponse) {
            case "EFF":
                executeRefundRequest(request, guid);
                break;
            case "ERR":
                break;
            default:
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
    }


    private String inquiryTransactionToBancomatPay(BPayRefundRequest request, String guid) throws Exception {
        InquiryTransactionStatusResponse inquiryTransactionStatusResponse;
        Long idPagoPa = request.getIdPagoPa();
        log.info("START requestInquiryTransactionToBancomatPay " + idPagoPa);

        inquiryTransactionStatusResponse = client.sendInquiryRequest(request, guid);
        if (inquiryTransactionStatusResponse == null || inquiryTransactionStatusResponse.getReturn() == null) {
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }

        log.info("END requestInquiryTransactionToBancomatPay " + idPagoPa);
        return inquiryTransactionStatusResponse.getReturn().getEsitoPagamento();

    }


    @Async
    public void executeRefundRequest(BPayRefundRequest request, String guid) throws RestApiException {
        StornoPagamentoResponse response;
        Long idPagoPa = request.getIdPagoPa();
        log.info("START executeRefundRequest for transaction " + idPagoPa + " with guid: " + guid);
        try {
            response = client.sendRefundRequest(request, guid);
            if (response == null || response.getReturn().getEsito() == null) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            EsitoVO esitoVO = response.getReturn().getEsito();
            log.info("Response from BPay sendRefundRequest - idPagopa: " + idPagoPa + " - esito: " + esitoVO.getCodice() + " - messaggio: " + esitoVO.getMessaggio());
            if (Boolean.FALSE.equals(esitoVO.isEsito())) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
        } catch (Exception e) {
            log.error("Exception calling BancomatPay with idPagopa: " + idPagoPa, e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        log.info("END executeRefundRequest " + idPagoPa);
    }

    @Async
    public void executePaymentRequest(BPayPaymentRequest request, String mdcInfo) throws RestApiException {
        InserimentoRichiestaPagamentoPagoPaResponse response;
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();
        log.info("START executePaymentRequest for transaction " + idPagoPa + " with guid: " + guid);
        try {
            response = client.sendPaymentRequest(request, guid);
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
            TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(TX_PROCESSING.getId(), null, null);
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
