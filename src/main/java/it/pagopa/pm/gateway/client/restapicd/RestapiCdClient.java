package it.pagopa.pm.gateway.client.restapicd;

import feign.*;
import it.pagopa.pm.gateway.dto.*;

@Headers({"Content-Type: application/json"})
public interface RestapiCdClient {

    @RequestLine("PATCH /v1/transactions/{id}")
    void updateTransaction(@Param Long id, TransactionUpdateRequest transactionUpdateRequest);

}
