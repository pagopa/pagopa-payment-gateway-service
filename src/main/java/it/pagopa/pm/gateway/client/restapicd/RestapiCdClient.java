package it.pagopa.pm.gateway.client.restapicd;

import feign.*;
import it.pagopa.pm.gateway.dto.*;

@Headers({"Content-Type: application/json"})
public interface RestapiCdClient {

    @RequestLine("PATCH /pp-restapi-CD/v1/transactions/update-status/{id}")
    void updateTransaction(@Param Long id, TransactionUpdateRequestData transactionUpdateRequest);

}
