package it.pagopa.pm.gateway.dto;

import lombok.*;

import javax.validation.constraints.*;
import it.pagopa.pm.gateway.dto.AuthMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ACKMessage {

    @NotEmpty
    private String outcome;

}
