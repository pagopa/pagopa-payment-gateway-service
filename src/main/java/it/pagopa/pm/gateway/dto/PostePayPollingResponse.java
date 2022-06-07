package it.pagopa.pm.gateway.dto;

import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import lombok.*;

@Data
public class PostePayPollingResponse {

    String channel;
    String urlRedirect;
    OutcomeEnum authOutcome;
    String clientResponseUrl;
    String error;

}
