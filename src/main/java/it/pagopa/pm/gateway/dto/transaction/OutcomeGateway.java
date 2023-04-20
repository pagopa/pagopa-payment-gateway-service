package it.pagopa.pm.gateway.dto.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class OutcomeGateway {
    @NotNull
    private AuthResultEnum outcome;

    private String rrn;

    private String authorizationCode;

    private String errorCode;

    public OutcomeGateway(AuthResultEnum outcome, String rrn, String authorizationCode, String errorCode) {
        this.outcome = outcome;
        this.rrn = rrn;
        this.authorizationCode = authorizationCode;
        this.errorCode = errorCode;
    }
}
