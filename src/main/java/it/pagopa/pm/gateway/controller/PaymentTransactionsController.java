package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.client.EsitoVO;
import it.pagopa.pm.gateway.client.InserimentoRichiestaPagamentoPagoPaResponse;
import it.pagopa.pm.gateway.client.ResponseInserimentoRichiestaPagamentoPagoPaVO;
import it.pagopa.pm.gateway.client.payment.gateway.client.BancomatPayClientV2;
import it.pagopa.pm.gateway.dto.ACKMessage;
import it.pagopa.pm.gateway.dto.AuthMessage;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentResponse;
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

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_BPAY;

@RestController
@Slf4j
public class PaymentTransactionsController {

    @Autowired
    BancomatPayClientV2 client;

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    @PutMapping(REQUEST_PAYMENTS_BPAY)
    public ACKMessage getPaymentAuthorization(AuthMessage authMessage) {
        ACKMessage response = new ACKMessage();
        return response;
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENTS_BPAY)
    public BancomatPayPaymentResponse requestPaymentToBancomatPay(@RequestBody BancomatPayPaymentRequest request) throws Exception {
        InserimentoRichiestaPagamentoPagoPaResponse response;
        Long idPagoPa = request.getIdPagoPa();

        log.info("START requestPaymentToBancomatPay " + idPagoPa);

        BancomatPayPaymentResponse bancomatPayPaymentResponse = new BancomatPayPaymentResponse();
        bancomatPayPaymentResponse.setOutcome(true);
        bancomatPayPaymentResponse.setIdPagoPa(idPagoPa);

        executeCallToBancomatPay(request);

        log.info("END requestPaymentToBancomatPay " + idPagoPa);

        return bancomatPayPaymentResponse;
    }

    @Async
    private InserimentoRichiestaPagamentoPagoPaResponse executeCallToBancomatPay(BancomatPayPaymentRequest request) throws BancomatPayClientException, RestApiInternalException {
        InserimentoRichiestaPagamentoPagoPaResponse response;
        Long idPagoPa = request.getIdPagoPa();

        try {
            response = client.getInserimentoRichiestaPagamentoPagoPaResponse(request);
        } catch (BancomatPayClientException bpce) {
            log.error("BancomatPayClientException in requestPaymentToBancomatPay idPagopa: " + idPagoPa, bpce);
            throw bpce;
        } catch (Exception e) {
            log.error("Exception in requestPaymentToBancomatPay idPagopa: " + idPagoPa, e);
            throw new RestApiInternalException(ExceptionsEnum.GENERIC_ERROR.getRestApiCode(), ExceptionsEnum.GENERIC_ERROR.getDescription());
        }

        BancomatPayPaymentResponse bancomatPayPaymentResponse = getBancomatPayPaymentResponse(response, idPagoPa);

        entityManager.persist(bancomatPayPaymentResponse);
        //TODO aggiorna stato transazione


        return response;

    }

    private BancomatPayPaymentResponse getBancomatPayPaymentResponse(InserimentoRichiestaPagamentoPagoPaResponse response, Long idPagoPa) {
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseReturnVO = response.getReturn();
        EsitoVO esitoVO = responseReturnVO.getEsito();
        BancomatPayPaymentResponse bancomatPayPaymentResponse = new BancomatPayPaymentResponse();
        bancomatPayPaymentResponse.setIdPagoPa(idPagoPa);
        bancomatPayPaymentResponse.setOutcome(esitoVO.isEsito());
        bancomatPayPaymentResponse.setMessage(esitoVO.getMessaggio());
        bancomatPayPaymentResponse.setErrorCode(esitoVO.getCodice());
        bancomatPayPaymentResponse.setCorrelationId(responseReturnVO.getCorrelationId());
        return bancomatPayPaymentResponse;
    }


}
