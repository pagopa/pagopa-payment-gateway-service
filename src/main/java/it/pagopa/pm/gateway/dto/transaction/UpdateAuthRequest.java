package it.pagopa.pm.gateway.dto.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
public class UpdateAuthRequest {
    @NotNull
    private AuthResultEnum authorizationResult;

    @NotBlank
    private String timestampOperation; //2022-02-11T13:00:00+01:00

    @NotBlank
    private String authorizationCode;

    public UpdateAuthRequest(AuthResultEnum authorizationResult, String authorizationCode) {
        this.authorizationResult = authorizationResult;
        this.authorizationCode = authorizationCode;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.timestampOperation = ZonedDateTime.now().format(formatter);
    }
}
