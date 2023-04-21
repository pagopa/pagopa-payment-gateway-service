package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.dto.creditcard.CreditCardResumeRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.dto.vpos.VposDeleteResponse;
import it.pagopa.pm.gateway.dto.vpos.VposResumeMethodResponse;
import it.pagopa.pm.gateway.service.*;
import it.pagopa.pm.gateway.utils.MdcUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;
import static it.pagopa.pm.gateway.constant.Headers.X_CLIENT_ID;
import static it.pagopa.pm.gateway.constant.Messages.*;

@RestController
@Slf4j
@NoArgsConstructor
@RequestMapping(REQUEST_PAYMENTS_VPOS)
public class CreditCardPaymentController {

    @Autowired
    private CcPaymentInfoService ccPaymentInfoService;

    @Autowired
    private VposService vposService;

    @Autowired
    private CcResumeStep1Service resumeStep1Service;

    @Autowired
    private CcResumeStep2Service resumeStep2Service;

    @Autowired
    private VposDeleteService deleteService;

    private String vposPollingUrl;

    @Autowired
    public CreditCardPaymentController(@Value("${vpos.polling.url}") String vposPollingUrl) {
        this.vposPollingUrl = vposPollingUrl;
    }

    @GetMapping(REQUEST_ID)
    public ResponseEntity<CcPaymentInfoResponse> getPaymentInfo(@PathVariable String requestId,
                                                                @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) {
        log.info("START - GET CreditCard request info for requestId: " + requestId);
        MdcUtils.setMdcFields(mdcFields);

        CcPaymentInfoResponse response = ccPaymentInfoService.getPaymentInfo(requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<StepZeroResponse> startCreditCardPayment(@RequestHeader(value = X_CLIENT_ID) String clientId,
                                                                   @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                   @Valid @RequestBody StepZeroRequest request) {
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
            httpStatus = HttpStatus.ACCEPTED;
        }
        return ResponseEntity.status(httpStatus).body(stepZeroResponse);
    }

    @PostMapping(REQUEST_PAYMENTS_RESUME_METHOD)
    public ResponseEntity<VposResumeMethodResponse> resumeCreditCardPayment(@RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                                            @PathVariable UUID requestId,
                                                                            @RequestBody CreditCardResumeRequest request) {
        log.info("START - POST {}{} info for requestId: {}", REQUEST_PAYMENTS_VPOS, REQUEST_PAYMENTS_RESUME_METHOD, requestId);
        MdcUtils.setMdcFields(mdcFields);
        VposResumeMethodResponse response = new VposResumeMethodResponse(requestId);
        resumeStep1Service.startResumeStep1(request, requestId.toString());

        log.info("END - POST {}{} info for requestId: {}", REQUEST_PAYMENTS_VPOS, REQUEST_PAYMENTS_RESUME_METHOD, requestId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping(REQUEST_PAYMENTS_RESUME_CHALLENGE)
    public ResponseEntity<String> resumeCreditCardPaymentStep2(@RequestHeader(required = false, value = MDC_FIELDS) String mdcFields,
                                                               @PathVariable String requestId) {
        log.info("START - POST {}{} info for requestId: {}", REQUEST_PAYMENTS_VPOS, REQUEST_PAYMENTS_RESUME_CHALLENGE, requestId);
        MdcUtils.setMdcFields(mdcFields);

        resumeStep2Service.startResumeStep2(requestId);
        log.info("END - POST {}{} info for requestId: {}", REQUEST_PAYMENTS_VPOS, REQUEST_PAYMENTS_RESUME_CHALLENGE, requestId);

        String vposPollingRedirect = vposPollingUrl + requestId;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(vposPollingRedirect)).build();
    }

    @DeleteMapping(REQUEST_ID)
    public ResponseEntity<VposDeleteResponse> deleteVposPayment(@PathVariable String requestId) {
        log.info("START - DELETE {} for requestId: {}", REQUEST_PAYMENTS_VPOS, requestId);
        VposDeleteResponse deleteResponse = deleteService.startDelete(requestId);
        log.info("START - DELETE {} for requestId: {}", REQUEST_PAYMENTS_VPOS, requestId);
        return buildResponseDelete(deleteResponse, requestId);
    }

    private ResponseEntity<VposDeleteResponse> buildResponseDelete(VposDeleteResponse deleteResponse, String requestId) {
        HttpStatus httpStatus = HttpStatus.OK;
        String error = deleteResponse.getError();
        if (Objects.nonNull(error)) {
            if (error.equals(REQUEST_ID_NOT_FOUND_MSG)) {
                httpStatus = HttpStatus.NOT_FOUND;
            }
            if (error.equals(GENERIC_REFUND_ERROR_MSG + requestId)) {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        return ResponseEntity.status(httpStatus).body(deleteResponse);
    }

}
