package it.pagopa.pm.gateway.dto;

import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import lombok.*;

@Data
public class PostePayPollingResponse {

    String channel;
    String urlRedirect;
    String logoResourcePath;
    String clientResponseUrl;
    OutcomeEnum authOutcome;
    String error;

}
