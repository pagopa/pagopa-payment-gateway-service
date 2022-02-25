package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.ACKMessage;
import it.pagopa.pm.gateway.dto.AuthMessage;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import it.pagopa.pm.gateway.entity.BPayPaymentResponseEntity;
import it.pagopa.pm.gateway.exception.BancomatPayClientException;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiInternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import java.lang.Exception;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_BPAY;

@RestController
@Slf4j
public class PaymentTransactionsController {

    @Autowired
    BancomatPayClient client;

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    @PutMapping(REQUEST_PAYMENTS_BPAY)
    public ACKMessage getPaymentAuthorization(AuthMessage authMessage) {
        return new ACKMessage();
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENTS_BPAY)
    public BPayPaymentResponseEntity requestPaymentToBancomatPay(@RequestBody BancomatPayPaymentRequest request) throws Exception {
        InserimentoRichiestaPagamentoPagoPaResponse response;
        Long idPagoPa = request.getIdPagoPa();

        log.info("START requestPaymentToBancomatPay " + idPagoPa);

        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setOutcome(true);
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);

        executeCallToBancomatPay(request);

        log.info("END requestPaymentToBancomatPay " + idPagoPa);

        return bPayPaymentResponseEntity;
    }

    @Async
    private void executeCallToBancomatPay(BancomatPayPaymentRequest request) throws BancomatPayClientException, RestApiInternalException {
        InserimentoRichiestaPagamentoPagoPaResponse response;
        Long idPagoPa = request.getIdPagoPa();
        try {
            response = client.sendPaymentRequest(request);
        } catch (BancomatPayClientException bpce) {
            log.error("BancomatPayClientException in requestPaymentToBancomatPay idPagopa: " + idPagoPa, bpce);
            throw bpce;
        } catch (Exception e) {
            log.error("Exception in requestPaymentToBancomatPay idPagopa: " + idPagoPa, e);
            throw new RestApiInternalException(ExceptionsEnum.GENERIC_ERROR.getRestApiCode(), ExceptionsEnum.GENERIC_ERROR.getDescription());
        }

        BPayPaymentResponseEntity bPayPaymentResponseEntity = getBancomatPayPaymentResponse(response, idPagoPa);

        entityManager.persist(bPayPaymentResponseEntity);
        //TODO aggiorna stato transazione
    }

    private BPayPaymentResponseEntity getBancomatPayPaymentResponse(InserimentoRichiestaPagamentoPagoPaResponse response, Long idPagoPa) {
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseReturnVO = response.getReturn();
        EsitoVO esitoVO = responseReturnVO.getEsito();
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        bPayPaymentResponseEntity.setOutcome(esitoVO.isEsito());
        bPayPaymentResponseEntity.setMessage(esitoVO.getMessaggio());
        bPayPaymentResponseEntity.setErrorCode(esitoVO.getCodice());
        bPayPaymentResponseEntity.setCorrelationId(responseReturnVO.getCorrelationId());
        return bPayPaymentResponseEntity;
    }

}
