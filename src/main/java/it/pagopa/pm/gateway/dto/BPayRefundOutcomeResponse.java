package it.pagopa.pm.gateway.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BPayRefundOutcomeResponse {

    private boolean needsRefund;

    private boolean refunded;

}
