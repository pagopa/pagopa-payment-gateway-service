package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.dto.creditcard.Step0CreditCardRequest;
import it.pagopa.pm.gateway.dto.creditcard.Step0CreditCardResponse;
import it.pagopa.pm.gateway.service.CCRequestPaymentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_CREDIT_CARD;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.Headers.X_CLIENT_ID;

@RestController
@Slf4j
@RequestMapping(REQUEST_PAYMENTS_CREDIT_CARD)
public class CreditCardPaymentController {

    @Autowired
    private CCRequestPaymentsService requestPaymentsService;

    @PostMapping()
    public ResponseEntity<Step0CreditCardResponse> requestPaymentsCreditCard(@RequestHeader(value = X_CLIENT_ID) String clientId,
                                                                             @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                             @RequestBody Step0CreditCardRequest request) {
        return requestPaymentsService.getRequestPayments(clientId, mdcFields, request);
    }


}
