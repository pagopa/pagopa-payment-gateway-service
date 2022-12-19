package it.pagopa.pm.gateway.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAuthRequest {
    @NotNull
    private AuthResultEnum authorizationResult;

    @NotBlank
    private String timestampOperation; //2022-02-11T13:00:00+01:00

    @NotBlank
    private String authorizationCode;
}
