package it.pagopa.pm.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XPayRefundResponse {

    String requestId;
    String refundOutcome;
    String error;

}
