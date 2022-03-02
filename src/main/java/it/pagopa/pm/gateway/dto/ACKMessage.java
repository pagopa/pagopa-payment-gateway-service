package it.pagopa.pm.gateway.dto;

import it.pagopa.pm.gateway.dto.enums.*;
import lombok.*;

import javax.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ACKMessage {

    @NotNull
    private OutcomeEnum outcome;

}
