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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;

import java.lang.Exception;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_BPAY;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.TX_ACCEPTED;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.TX_PROCESSING;

@RestController
@Slf4j
public class PaymentTransactionsController {

    @Autowired
    BancomatPayClient client;

    @Autowired
    BPayPaymentResponseRepository bPayPaymentResponseRepository;

    @Autowired
    RestapiCdClientImpl restapiCdClient;

    @PutMapping(REQUEST_PAYMENTS_BPAY)
    public ACKMessage updateTransaction(@RequestBody AuthMessage authMessage, @RequestHeader("X-Correlation-ID") String correlationId) throws RestApiException {
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByCorrelationId(correlationId);
        if (alreadySaved == null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        } else if (Boolean.TRUE.equals(alreadySaved.getIsProcessed())) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
        }
        TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(TX_ACCEPTED.getId(), authMessage.getAuthCode(), null);
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
        }
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENTS_BPAY)
    public BPayPaymentResponseEntity requestPaymentToBancomatPay(@RequestBody BPayPaymentRequest request) throws Exception {
        Long idPagoPa = request.getIdPagoPa();
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (alreadySaved != null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
        }
        log.info("START requestPaymentToBancomatPay " + idPagoPa);
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setOutcome(true);
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        executeCallToBancomatPay(request);
        try {
            TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(TX_PROCESSING.getId(), null, null);
            restapiCdClient.callTransactionUpdate(idPagoPa, transactionUpdate);
        } catch (FeignException e) {
            log.error("Exception calling RestapiCD transaction update", e);
            throw new RestApiException(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR, e.status());
        }
        log.info("END requestPaymentToBancomatPay " + idPagoPa);
        return bPayPaymentResponseEntity;
    }

    @Async
    public void executeCallToBancomatPay(BPayPaymentRequest request) throws RestApiException {
        InserimentoRichiestaPagamentoPagoPaResponse response;
        Long idPagoPa = request.getIdPagoPa();
        try {
            response = client.sendPaymentRequest(request);
        } catch (Exception e) {
            log.error("Exception calling BancomatPay with idPagopa: " + idPagoPa, e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        BPayPaymentResponseEntity bPayPaymentResponseEntity = getBancomatPayPaymentResponse(response, idPagoPa);
        bPayPaymentResponseRepository.save(bPayPaymentResponseEntity);
        //TODO aggiorna stato transazione
    }

    private BPayPaymentResponseEntity getBancomatPayPaymentResponse(InserimentoRichiestaPagamentoPagoPaResponse response, Long idPagoPa) {
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseReturnVO = response.getReturn();
        String clientGuid = null;
        if (responseReturnVO.getContesto()!=null){
            clientGuid  = responseReturnVO.getContesto().getGuid();
        }
        EsitoVO esitoVO = responseReturnVO.getEsito();
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        bPayPaymentResponseEntity.setOutcome(esitoVO.isEsito());
        bPayPaymentResponseEntity.setMessage(esitoVO.getMessaggio());
        bPayPaymentResponseEntity.setErrorCode(esitoVO.getCodice());
        bPayPaymentResponseEntity.setCorrelationId(responseReturnVO.getCorrelationId());
        bPayPaymentResponseEntity.setClientGuid(clientGuid);
        return bPayPaymentResponseEntity;
    }

}
