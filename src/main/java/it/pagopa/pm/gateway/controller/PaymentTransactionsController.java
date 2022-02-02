package it.pagopa.pm.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentTransactionsController {
 
	@GetMapping("/test")
	public String index() {
		return "test";
	}
}
