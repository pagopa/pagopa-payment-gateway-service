package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.client.InserimentoRichiestaPagamentoPagoPaResponse;
import it.pagopa.pm.gateway.client.payment.gateway.client.BancomatPayClientV2;
import it.pagopa.pm.gateway.dto.ACKMessage;
import it.pagopa.pm.gateway.dto.AuthMessage;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentResponse;
import it.pagopa.pm.gateway.exception.BancomatPayClientException;
import it.pagopa.pm.gateway.exception.BancomatPayOutcomeException;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiInternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.BPAY;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS;

@RestController
public class PaymentTransactionsController {

	@Autowired
	BancomatPayClientV2 client;

	@PutMapping(REQUEST_PAYMENTS + BPAY)
	public ACKMessage getPaymentAuthorization(AuthMessage authMessage) {
		ACKMessage response = new ACKMessage();
		return response;
	}

	@GetMapping("/test")
	public String index() {
		return "test";
	}

	@PostMapping(REQUEST_PAYMENTS + BPAY)
	public BancomatPayPaymentResponse requestPaymentToBancomatPay(@RequestBody BancomatPayPaymentRequest request ) throws Exception {

		InserimentoRichiestaPagamentoPagoPaResponse response;

		//Error in client communication with Bpay -> HTTP 502
		try {
			response = client.getInserimentoRichiestaPagamentoPagoPaResponse(request);
		} catch (BancomatPayClientException bpce){
         	throw bpce;
		} catch (Exception e ){
			throw new RestApiInternalException(ExceptionsEnum.GENERIC_ERROR.getRestApiCode(), ExceptionsEnum.GENERIC_ERROR.getDescription());
		}

		Boolean outcome = response.getReturn().getEsito().isEsito();
		//if (!outcome){
		 //	throw new BancomatPayOutcomeException(ExceptionsEnum.BPAY_SERVICE_NEGATIVE_OUTCOME_ERROR.getRestApiCode(), ExceptionsEnum.BPAY_SERVICE_NEGATIVE_OUTCOME_ERROR.getDescription());
		//};

		BancomatPayPaymentResponse bancomatPayPaymentResponse = new BancomatPayPaymentResponse();
		bancomatPayPaymentResponse.setOutcome(Boolean.toString(outcome));
		bancomatPayPaymentResponse.setCorrelationId(response.getReturn().getCorrelationId());
		bancomatPayPaymentResponse.setErrorCode(response.getReturn().getEsito().getCodice());
		bancomatPayPaymentResponse.setMessage(response.getReturn().getEsito().getMessaggio());


        return bancomatPayPaymentResponse;
	}


}
