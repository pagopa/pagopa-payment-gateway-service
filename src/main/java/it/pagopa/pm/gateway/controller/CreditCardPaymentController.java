package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.service.CcPaymentInfoService;
import it.pagopa.pm.gateway.utils.MdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_ID;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_CREDIT_CARD;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;

@RestController
@Slf4j
@RequestMapping(REQUEST_PAYMENTS_CREDIT_CARD)
public class CreditCardPaymentController {
    @Autowired
    private CcPaymentInfoService ccPaymentInfoService;

    @GetMapping(REQUEST_ID)
    public ResponseEntity<CcPaymentInfoResponse> getPaymentInfo(@PathVariable String requestId,
                                                                @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) {
        log.info("START - GET CreditCard request info for requestId: " + requestId);
        MdcUtils.setMdcFields(mdcFields);

        CcPaymentInfoResponse response = ccPaymentInfoService.getPaymentoInfo(requestId);
        return ResponseEntity.ok(response);
    }
}
