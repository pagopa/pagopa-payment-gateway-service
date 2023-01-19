package it.pagopa.pm.gateway.dto.vpos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VposDeleteResponse {

    private String requestId;
    private String refundOutcome;
    private String status;
    private String error;
}
