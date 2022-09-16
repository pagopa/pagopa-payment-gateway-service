package it.pagopa.pm.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostePayRefundResponse {

        private String requestId;
        private String paymentId;
        private String refundOutcome;
        private String error;
        private boolean needsRefund;

}
