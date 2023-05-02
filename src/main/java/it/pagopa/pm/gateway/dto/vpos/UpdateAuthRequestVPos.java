package it.pagopa.pm.gateway.dto.vpos;

import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class UpdateAuthRequestVPos {
    @NotNull
    private OutcomeVposGatewayRequest outcomeGateway;

    @NotNull
    private OffsetDateTime timestampOperation;

    public UpdateAuthRequestVPos(String paymentGatewayType, AuthResultEnum outcome, String rrn, String authorizationCode, String errorCode) {
        this.outcomeGateway = new OutcomeVposGatewayRequest(paymentGatewayType, outcome, rrn, authorizationCode, errorCode);
        this.timestampOperation = OffsetDateTime.now();
    }
}
