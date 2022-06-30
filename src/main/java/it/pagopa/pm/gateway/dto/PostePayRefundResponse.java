package it.pagopa.pm.gateway.dto;

import lombok.Data;

@Data
public class PostePayRefundResponse {

        String requestId;
        String paymentId;
        String refundOutcome;
        String error;

}
