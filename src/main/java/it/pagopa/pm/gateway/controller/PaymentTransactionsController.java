package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.AuthMessage;
import it.pagopa.pm.gateway.dto.BPayPaymentRequest;
import it.pagopa.pm.gateway.entity.BPayPaymentResponseEntity;
import it.pagopa.pm.gateway.exception.BancomatPayClientException;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiInternalException;
import it.pagopa.pm.gateway.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;

import java.lang.Exception;

import static it.pagopa.pm.gateway.constant.ApiPaths.ID_PATH_PARAM;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_BPAY;

@RestController
@Slf4j
public class PaymentTransactionsController {

    @Autowired
    BancomatPayClient client;

    @Autowired
    BPayPaymentResponseRepository bPayPaymentResponseRepository;

    @Transactional
    @PutMapping(REQUEST_PAYMENTS_BPAY + ID_PATH_PARAM)
    public ResponseEntity updateTransaction(@RequestBody AuthMessage authMessage, @PathVariable Long id) {
        return ResponseEntity.ok().build();
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENTS_BPAY)
    public BPayPaymentResponseEntity requestPaymentToBancomatPay(@RequestBody BPayPaymentRequest request) throws Exception {
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
    public void executeCallToBancomatPay(BPayPaymentRequest request) throws BancomatPayClientException, RestApiInternalException {
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
        bPayPaymentResponseRepository.save(bPayPaymentResponseEntity);
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
