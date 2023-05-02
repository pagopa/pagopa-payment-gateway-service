package it.pagopa.pm.gateway.dto.xpay;

import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class UpdateAuthRequestXPay {
    @NotNull
    private OutcomeXpayGatewayRequest outcomeGateway;

    @NotNull
    private OffsetDateTime timestampOperation;

    public UpdateAuthRequestXPay(String paymentGatewayType, AuthResultEnum outcome, String authorizationCode, String errorCode) {
        this.outcomeGateway = new OutcomeXpayGatewayRequest(paymentGatewayType, outcome, authorizationCode, errorCode);
        this.timestampOperation = OffsetDateTime.now();
    }
}
