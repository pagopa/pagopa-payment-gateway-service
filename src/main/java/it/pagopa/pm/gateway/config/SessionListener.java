package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.restapicd.*;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.entity.*;
import it.pagopa.pm.gateway.repository.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import javax.servlet.http.*;

import static it.pagopa.pm.gateway.constant.SessionParams.ID_PAGOPA_PARAM;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.*;


@Slf4j
@Component
public class SessionListener implements HttpSessionListener {

    @Autowired
    RestapiCdClientImpl restapiCdClient;

    @Autowired
    BPayPaymentResponseRepository bPayPaymentResponseRepository;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        Long idPagoPa = (Long) event.getSession().getAttribute(ID_PAGOPA_PARAM);
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (!alreadySaved.getIsProcessed()) {
            restapiCdClient.callTransactionUpdate(idPagoPa, new TransactionUpdateRequest(TX_TO_BE_REVERTED.getId(), null, null));
            alreadySaved.setIsProcessed(true);
            bPayPaymentResponseRepository.save(alreadySaved);
        }
    }

}