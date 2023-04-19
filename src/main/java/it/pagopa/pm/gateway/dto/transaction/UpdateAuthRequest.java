package it.pagopa.pm.gateway.dto.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class UpdateAuthRequest {
    @NotNull
    private AuthResultEnum authorizationResult;

    @NotNull
    private OffsetDateTime timestampOperation;

    @NotBlank
    private String authorizationCode;

    private String rrn;

    public UpdateAuthRequest(AuthResultEnum authorizationResult, String authorizationCode, String rrn) {
        this.authorizationResult = authorizationResult;
        this.authorizationCode = authorizationCode;
        this.timestampOperation = OffsetDateTime.now();
        this.rrn = rrn;
    }
}
