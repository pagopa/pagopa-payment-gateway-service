package it.pagopa.pm.gateway.dto.xpay;

import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class OutcomeXpayGatewayRequest {
    @NotNull
    private String paymentGatewayType;

    @NotNull
    private AuthResultEnum outcome;

    private String authorizationCode;

    private String errorCode;

    public OutcomeXpayGatewayRequest(String paymentGatewayType, AuthResultEnum outcome, String authorizationCode, String errorCode) {
        this.paymentGatewayType = paymentGatewayType;
        this.outcome = outcome;
        this.authorizationCode = authorizationCode;
        this.errorCode = errorCode;
    }
}
