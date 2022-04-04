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
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.*;
import javax.transaction.Transactional;

import java.lang.Exception;
import java.net.*;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_BPAY;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_REFUNDS_BPAY;
import static it.pagopa.pm.gateway.constant.SessionParams.ID_PAGOPA_PARAM;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.*;

@RestController
@Slf4j
public class PaymentTransactionsController {

    @Autowired
    BancomatPayClient client;

    @Autowired
    BPayPaymentResponseRepository bPayPaymentResponseRepository;

    @Autowired
    RestapiCdClientImpl restapiCdClient;

    @Value("${bancomatPay.session.timeout.s:60}")
    public String BPAY_SESSION_TIMEOUT;

    @PutMapping(REQUEST_PAYMENTS_BPAY)
    public ACKMessage updateTransaction(@RequestBody AuthMessage authMessage, @RequestHeader("X-Correlation-ID") String correlationId) throws RestApiException {
        log.info("START Update transaction request for correlation-id: " + correlationId + ": " + authMessage);
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByCorrelationId(correlationId);
        if (alreadySaved == null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        } else if (Boolean.TRUE.equals(alreadySaved.getIsProcessed())) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
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
    public BPayPaymentResponseEntity requestPaymentToBancomatPay(@RequestBody BPayPaymentRequest request, HttpSession session) throws Exception {
        Long idPagoPa = request.getIdPagoPa();
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (alreadySaved != null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
        }
        log.info("START requestPaymentToBancomatPay " + idPagoPa);
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setOutcome(true);
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        executePaymentRequest(request);
        session.setMaxInactiveInterval(Integer.parseInt(BPAY_SESSION_TIMEOUT));
        session.setAttribute(ID_PAGOPA_PARAM, idPagoPa);
        log.info("END requestPaymentToBancomatPay " + idPagoPa);
        return bPayPaymentResponseEntity;
    }

    @Transactional
    @PostMapping(REQUEST_REFUNDS_BPAY)
    public void requestRefundToBancomatPay(@RequestBody BPayRefundRequest request) throws Exception {
        Long idPagoPa = request.getIdPagoPa();
        log.info("START requestRefundToBancomatPay " + idPagoPa);
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setOutcome(true);
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        executeRefundRequest(request);
        log.info("END requestRefundToBancomatPay " + idPagoPa);
    }

    @Async
    public void executeRefundRequest(BPayRefundRequest request) throws RestApiException {
        StornoPagamentoResponse response;
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();
        log.info("START executeRefundRequest for transaction " + idPagoPa + " with guid: " + guid);
        try {
            response = client.sendRefundRequest(request, guid);
            if (response == null || response.getReturn().getEsito() == null) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            EsitoVO esitoVO = response.getReturn().getEsito();
            log.info("Response from BPay sendRefundRequest - idPagopa: " + idPagoPa + " - esito: " + esitoVO.getCodice() + " - messaggio: " + esitoVO.getMessaggio());
        } catch (Exception e) {
            log.error("Exception calling BancomatPay with idPagopa: " + idPagoPa, e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        log.info("END executeRefundRequest " + idPagoPa);
    }

    @Async
    public void executePaymentRequest(BPayPaymentRequest request) throws RestApiException {
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
            log.info("Response from BPay sendPaymentRequest - idPagopa: " + idPagoPa + " - esito: " + esitoVO.getCodice() + " - messaggio: " + esitoVO.getMessaggio());
        } catch (Exception e) {
            log.error("Exception calling BancomatPay with idPagopa: " + idPagoPa, e);
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new RestApiException(ExceptionsEnum.TIMEOUT);
            }
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        BPayPaymentResponseEntity bPayPaymentResponseEntity = convertBpayPaymentResponseToEntity(response, idPagoPa, guid);
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

    private BPayPaymentResponseEntity convertBpayPaymentResponseToEntity(InserimentoRichiestaPagamentoPagoPaResponse response, Long idPagoPa, String guid) {
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseReturnVO = response.getReturn();
        EsitoVO esitoVO = responseReturnVO.getEsito();
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        bPayPaymentResponseEntity.setOutcome(esitoVO.isEsito());
        bPayPaymentResponseEntity.setMessage(esitoVO.getMessaggio());
        bPayPaymentResponseEntity.setErrorCode(esitoVO.getCodice());
        bPayPaymentResponseEntity.setCorrelationId(responseReturnVO.getCorrelationId());
        bPayPaymentResponseEntity.setClientGuid(guid);
        return bPayPaymentResponseEntity;
    }

}
