package it.pagopa.pm.gateway.dto;

import lombok.*;

import javax.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ACKMessage {

    @NotEmpty
    private String outcome;

}
