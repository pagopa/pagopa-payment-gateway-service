package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XPayPollingResponse {

    private String status;
    private XPayPollingResponseError error;
    private String html;
    private String authCode;
    private String redirectUrl;
    private String requestId;
}
