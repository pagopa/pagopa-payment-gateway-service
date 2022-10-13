package it.pagopa.pm.gateway.client.restapicd;

import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import it.pagopa.pm.gateway.dto.PostePayPatchRequestData;
import it.pagopa.pm.gateway.dto.TransactionUpdateRequestData;

import java.util.Map;

@Headers({"Content-Type: application/json"})
public interface RestapiCdClient {

    @RequestLine("PATCH /pp-restapi-CD/v1/transactions/update-status/{id}")
    void updateTransaction(@Param Long id, @HeaderMap Map<String, Object> headerMap, TransactionUpdateRequestData transactionUpdateRequest);

    @RequestLine("PATCH /pp-restapi-CD/v2/transactions/{id}")
    String callPatchTransactionV2(@Param Long id, @HeaderMap Map<String, Object> headerMap, PostePayPatchRequestData postePayPatchRequest);
}
