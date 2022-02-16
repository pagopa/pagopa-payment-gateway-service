package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.client.InserimentoRichiestaPagamentoPagoPaResponse;
import it.pagopa.pm.gateway.client.payment.gateway.client.BancomatPayClientV2;
import it.pagopa.pm.gateway.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENT;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS;
import static it.pagopa.pm.gateway.constant.ApiPaths.BPAY;

@RestController
public class PaymentTransactionsController {

	@Autowired
	BancomatPayClientV2 client;

	/*@PostMapping(REQUEST_PAYMENTS + BPAY)
	public ACKMessage getPaymentAuthorization(AuthMessage authMessage) {
		ACKMessage response = new ACKMessage();
		return response;
	} */

	@GetMapping("/test")
	public String index() {
		return "test";
	}

	@PostMapping(REQUEST_PAYMENTS + BPAY)
	public void requestPaymentToBancomatPay(@RequestHeader String xCorrelationId,
											@RequestBody BancomatPayPaymentRequest request ) throws Exception {
		InserimentoRichiestaPagamentoPagoPaResponse response = null;

		try {
			response = client.getInserimentoRichiestaPagamentoPagoPaResponse(request);
		} catch (Exception e){
         //throw 502
		}

		//questo dato va salvato

		if (!response.getReturn().getEsito().isEsito()){
			//throw 424
		};

		String correlationId = response.getReturn().getCorrelationId();
        try {
        	//salva correlationID
		} catch (Exception e){
        	//throw 500
		}



	}


}
