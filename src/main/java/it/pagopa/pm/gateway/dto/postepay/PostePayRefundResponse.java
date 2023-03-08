package it.pagopa.pm.gateway.dto.postepay;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostePayRefundResponse {

        String requestId;
        String paymentId;
        String refundOutcome;
        String error;

}
