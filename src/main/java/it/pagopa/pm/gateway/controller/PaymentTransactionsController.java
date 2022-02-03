package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.dto.*;
import org.springframework.web.bind.annotation.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.BPAY;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENT;

@RestController
public class PaymentTransactionsController {

	@PostMapping(REQUEST_PAYMENT + BPAY)
	public ACKMessage getPaymentAuthorization(AuthMessage authMessage) {
		ACKMessage response = new ACKMessage();
		return response;
	}

	@GetMapping("/test")
	public String index() {
		return "test";
	}

}
