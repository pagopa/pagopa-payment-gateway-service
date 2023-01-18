package it.pagopa.pm.gateway.dto.vpos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CcPaymentInfoResponse {
    private String status;
    private String responseType;
    private String requestId;
    private String vposUrl;
    private String clientReturnUrl;
}
