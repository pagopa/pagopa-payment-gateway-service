package it.pagopa.pm.gateway.controller.postepay;

import feign.FeignException;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.dto.ACKError;
import it.pagopa.pm.gateway.dto.ACKMessage;
import it.pagopa.pm.gateway.dto.AuthMessage;
import it.pagopa.pm.gateway.dto.enums.EndpointEnum;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import lombok.extern.slf4j.Slf4j;
import okio.Timeout;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENT_POSTEPAY;
import static it.pagopa.pm.gateway.constant.Headers.X_CORRELATION_ID;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
public class PostePayPaymentTransactionsController {

    @Autowired
    private RestapiCdClientImpl restapiCdClient;
    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @PutMapping(REQUEST_PAYMENT_POSTEPAY)
    public ACKMessage closePayment(@RequestBody AuthMessage authMessage,
                                   @RequestHeader(X_CORRELATION_ID) String correlationId) {
        MDC.clear();
        if (Objects.isNull(authMessage) || authMessage.getAuthOutcome() == null || StringUtils.isBlank(correlationId)) {
            return new ACKError(OutcomeEnum.KO, ExceptionsEnum.MISSING_FIELDS.getDescription());
        }
        log.info("START Close payment request for correlation-id: " + correlationId + ": " + authMessage);
        PaymentRequestEntity postePayPaymentRequest = paymentRequestRepository.findByCorrelationIdAndRequestEndpoint(
                correlationId, EndpointEnum.POSTEPAY.getValue());
        if (Objects.isNull(postePayPaymentRequest)) {
            return new ACKError(OutcomeEnum.KO, ExceptionsEnum.TRANSACTION_NOT_FOUND.getDescription());
        } else {
            setMdcFields(postePayPaymentRequest.getMdcInfo());
            if (postePayPaymentRequest.getIsProcessed()) {
                return new ACKError(OutcomeEnum.KO, ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED.getDescription());
            }
        }
        try {
            String closePayment = restapiCdClient.callClosePayment(postePayPaymentRequest.getIdTransaction(),
                    authMessage.getAuthOutcome() == OutcomeEnum.OK);
            postePayPaymentRequest.setIsProcessed(true);
            paymentRequestRepository.save(postePayPaymentRequest);
            if (closePayment.equals(OutcomeEnum.KO.toString())) {
                return new ACKError(OutcomeEnum.KO, ExceptionsEnum.GENERIC_ERROR.getDescription());
            }
        } catch (FeignException fe) {
            log.error("Exception calling RestapiCD to close payment", fe);
            return new ACKError(OutcomeEnum.KO, ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR.getDescription());
        } catch (Exception e) {
            log.error("Exception closing payment", e);
            return new ACKError(OutcomeEnum.KO, ExceptionsEnum.GENERIC_ERROR.getDescription());
        } finally {
            log.info("END Update transaction request for correlation-id: " + correlationId);
        }
        return new ACKMessage(OutcomeEnum.OK);
    }

}
