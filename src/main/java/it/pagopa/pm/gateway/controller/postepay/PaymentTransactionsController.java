package it.pagopa.pm.gateway.controller.postepay;

import feign.FeignException;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.dto.ACKError;
import it.pagopa.pm.gateway.dto.ACKMessage;
import it.pagopa.pm.gateway.dto.AuthMessage;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_PPAY;
import static it.pagopa.pm.gateway.constant.Headers.X_CORRELATION_ID;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
public class PaymentTransactionsController {

    @Autowired
    private RestapiCdClientImpl restapiCdClient;

    @PutMapping(REQUEST_PAYMENTS_PPAY)
    public ACKMessage closePayment(@RequestBody AuthMessage authMessage,
                                   @RequestHeader(X_CORRELATION_ID) String correlationId) throws RestApiException {
        MDC.clear();
        log.info("START Close payment request for correlation-id: " + correlationId + ": " + authMessage);
        //TODO invocare repository per cercare autorizzazione tramite correlationId
        Object postePayPaymentResponse = new Object();
        if (Objects.isNull(postePayPaymentResponse)) {
            return new ACKError(OutcomeEnum.KO, ExceptionsEnum.TRANSACTION_NOT_FOUND.getDescription());
        } else {
            //TODO salvare le info MDC e controllare che sia già stata processata
            setMdcFields(postePayPaymentResponse.toString());
            if (Boolean.TRUE.equals(postePayPaymentResponse.equals(new Object()))) {
                return new ACKError(OutcomeEnum.KO, ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED.getDescription());
            }
        }
        //TODO aggiornare la chiamata non appena si hanno maggiori info
        try {
            restapiCdClient.callClosePayment(Long.valueOf(postePayPaymentResponse.toString()), true);
            //TODO impostare l'autorizzazione a processata e salvare a DB
        } catch (FeignException fe) {
            log.error("Exception calling RestapiCD to update transaction", fe);
            return new ACKError(OutcomeEnum.KO, ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR.getDescription());
        } catch (Exception e) {
            log.error("Exception updating transaction", e);
            return new ACKError(OutcomeEnum.KO, ExceptionsEnum.GENERIC_ERROR.getDescription());
        } finally {
            log.info("END Update transaction request for correlation-id: " + correlationId);
        }
        return new ACKMessage(OutcomeEnum.OK);
    }

}
