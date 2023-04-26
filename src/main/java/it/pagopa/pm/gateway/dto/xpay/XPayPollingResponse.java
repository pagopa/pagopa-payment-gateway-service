package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XPayPollingResponse {

    @JsonProperty(value = "status")
    private PaymentRequestStatusEnum paymentRequestStatusEnum;
    private String errorDetail;
    private String html;
    private String redirectUrl;
    @JsonProperty(value = "paymentAuthorizationId")
    private String requestId;
    private OutcomeXpayGateway outcomeXpayGateway;
}
