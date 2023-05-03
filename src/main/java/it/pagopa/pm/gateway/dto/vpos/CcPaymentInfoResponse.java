package it.pagopa.pm.gateway.dto.vpos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@JsonInclude(NON_NULL)
public class CcPaymentInfoResponse {
    @JsonProperty(value = "status")
    private PaymentRequestStatusEnum paymentRequestStatusEnum;
    @JsonProperty(value = "responseType")
    private ThreeDS2ResponseTypeEnum threeDS2ResponseTypeEnum;
    private String requestId;
    private String vposUrl;
    private String redirectUrl;
    private String threeDsMethodData;
    private String creq;
    private OutcomeVposGatewayResponse outcomeVposGatewayResponse;
}
