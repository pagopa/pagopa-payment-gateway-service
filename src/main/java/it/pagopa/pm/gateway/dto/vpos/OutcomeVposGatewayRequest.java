package it.pagopa.pm.gateway.dto.vpos;

import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import it.pagopa.pm.gateway.dto.xpay.OutcomeXpayGatewayRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class OutcomeVposGatewayRequest extends OutcomeXpayGatewayRequest {
    @NotNull
    private String rrn;

    public OutcomeVposGatewayRequest(String paymentGatewayType, AuthResultEnum outcome, String rrn, String authorizationCode, String errorCode) {
        super(paymentGatewayType, outcome, authorizationCode, errorCode);
        this.rrn = rrn;
    }
}
