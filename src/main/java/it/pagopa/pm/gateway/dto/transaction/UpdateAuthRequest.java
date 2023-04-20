package it.pagopa.pm.gateway.dto.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class UpdateAuthRequest {
    @NotNull
    private OutcomeGateway outcomeGateway;

    @NotNull
    private OffsetDateTime timestampOperation;

    public UpdateAuthRequest(AuthResultEnum outcome, String rrn, String authorizationCode, String errorCode) {
        this.outcomeGateway = new OutcomeGateway(outcome, rrn, authorizationCode, errorCode);
        this.timestampOperation = OffsetDateTime.now();
    }
}
