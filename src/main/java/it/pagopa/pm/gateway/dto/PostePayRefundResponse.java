package it.pagopa.pm.gateway.dto;

import lombok.Data;

@Data
public class PostePayRefundResponse {

        Long transactionId;
        String  paymentId;
        String refundOutcome;
        String error;

}
