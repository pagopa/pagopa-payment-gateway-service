package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.dto.creditcard.CreditCardResumeRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.service.CcPaymentInfoService;
import it.pagopa.pm.gateway.service.CcResumeService;
import it.pagopa.pm.gateway.service.VposService;
import it.pagopa.pm.gateway.utils.MdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.Headers.X_CLIENT_ID;
import static it.pagopa.pm.gateway.constant.Messages.*;

@RestController
@Slf4j
@RequestMapping(REQUEST_PAYMENTS_CREDIT_CARD)
public class CreditCardPaymentController {

    @Value("${vpos.response.urlredirect}")
    private String responseUrlRedirect;

    @Autowired
    private CcPaymentInfoService ccPaymentInfoService;

    @Autowired
    private VposService vposService;

    @Autowired
    private CcResumeService ccResumeService;

    @GetMapping(REQUEST_ID)
    public ResponseEntity<CcPaymentInfoResponse> getPaymentInfo(@PathVariable String requestId,
                                                                @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) {
        log.info("START - GET CreditCard request info for requestId: " + requestId);
        MdcUtils.setMdcFields(mdcFields);

        CcPaymentInfoResponse response = ccPaymentInfoService.getPaymentoInfo(requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<StepZeroResponse> startCreditCardPayment(@RequestHeader(value = X_CLIENT_ID) String clientId,
                                                                   @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                   @RequestBody StepZeroRequest request) {
        StepZeroResponse stepZeroResponse = vposService.startCreditCardPayment(clientId, mdcFields, request);
        return buildResponseStep0(stepZeroResponse);
    }

    private ResponseEntity<StepZeroResponse> buildResponseStep0(StepZeroResponse stepZeroResponse) {
        String errorMessage = stepZeroResponse.getError();
        HttpStatus httpStatus;
        if (StringUtils.isNotBlank(errorMessage)) {
            if (errorMessage.equals(BAD_REQUEST_MSG) || errorMessage.equals(BAD_REQUEST_MSG_CLIENT_ID)) {
                httpStatus = HttpStatus.BAD_REQUEST;
            } else if (errorMessage.equals(TRANSACTION_ALREADY_PROCESSED_MSG)) {
                httpStatus = HttpStatus.UNAUTHORIZED;
            } else {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            httpStatus = HttpStatus.OK;
        }
        return ResponseEntity.status(httpStatus).body(stepZeroResponse);
    }

    @PostMapping(REQUEST_PAYMENTS_RESUME)
    public ResponseEntity<String> resumeCreditCardPayment(@RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                          @PathVariable String requestId,
                                                          @RequestBody(required = false) CreditCardResumeRequest request) {
        log.info("START - POST {}{} info for requestId: {}", REQUEST_PAYMENTS_CREDIT_CARD, REQUEST_PAYMENTS_RESUME, requestId);
        MdcUtils.setMdcFields(mdcFields);
        String urlRedirect = StringUtils.join(responseUrlRedirect, requestId);
        ccResumeService.startResume(request, requestId);
        log.info("END - POST {}{} info for requestId: {}", REQUEST_PAYMENTS_CREDIT_CARD, REQUEST_PAYMENTS_RESUME, requestId);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(urlRedirect)).build();
    }

}
