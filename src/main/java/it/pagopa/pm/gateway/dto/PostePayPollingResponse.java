package it.pagopa.pm.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.StatusErrorCodeOutcomeEnum;
import lombok.*;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostePayPollingResponse {

    String channel;
    String urlRedirect;
    String logoResourcePath;
    String clientResponseUrl;
    OutcomeEnum authOutcome;
    String error;
    StatusErrorCodeOutcomeEnum statusErrorCodeOutcome;

}
