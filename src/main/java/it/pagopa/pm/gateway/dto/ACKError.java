package it.pagopa.pm.gateway.dto;

import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ACKError extends ACKMessage {

    private String error;

    public ACKError(OutcomeEnum ko, String error) {
        super(ko);
        this.error = error;
    }

}
