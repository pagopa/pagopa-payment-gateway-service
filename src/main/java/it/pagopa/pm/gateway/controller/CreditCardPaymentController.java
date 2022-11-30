package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.service.VposService;
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
    private VposService vposService;

    @PostMapping()
    public ResponseEntity<StepZeroResponse> startCreditCardPayment(@RequestHeader(value = X_CLIENT_ID) String clientId,
                                                                   @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                   @RequestBody StepZeroRequest request) {
        return vposService.startCreditCardPayment(clientId, mdcFields, request);
    }


}
