package it.pagopa.pm.gateway.dto;

import com.fasterxml.jackson.annotation.*;
import it.pagopa.pm.gateway.dto.enums.*;
import lombok.*;

import javax.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthMessage {

    @NotNull
    @JsonProperty("auth_outcome")
    private OutcomeEnum authOutcome;

    @JsonProperty("auth_code")
    private String authCode;

}
