package it.pagopa.pm.gateway.client.restapicd;

import feign.*;
import it.pagopa.pm.gateway.dto.*;

import java.util.*;

@Headers({"Content-Type: application/json"})
public interface RestapiCdClient {

    @RequestLine("PATCH /pp-restapi-CD/v1/transactions/update-status/{id}")
    void updateTransaction(@Param Long id, @HeaderMap Map<String, Object> headerMap, TransactionUpdateRequestData transactionUpdateRequest);

    @RequestLine("POST /pp-restapi-CD/v1/payments/close-payment/{idTransaction}?outcome={outcome}")
    String closePayment(@Param Long idTransaction, @Param boolean outcome, @HeaderMap Map<String, Object> headerMap);
}
