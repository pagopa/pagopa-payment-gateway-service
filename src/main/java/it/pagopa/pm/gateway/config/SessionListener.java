package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.client.restapicd.*;
import it.pagopa.pm.gateway.dto.*;
import lombok.extern.log4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import javax.servlet.http.*;

import static it.pagopa.pm.gateway.constant.SessionParams.ID_PAGOPA_PARAM;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.*;


@Log4j
@Component
public class SessionListener implements HttpSessionListener {

    @Autowired
    RestapiCdClientImpl restapiCdClient;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(TX_TO_BE_REVERTED.getId(), null, null);
        restapiCdClient.callTransactionUpdate((Long)event.getSession().getAttribute(ID_PAGOPA_PARAM), transactionUpdate);
    }

}