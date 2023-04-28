package it.pagopa.pm.gateway.dto.vpos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.VposErrorCodeEnum;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@JsonInclude(NON_NULL)
public class OutcomeVposGateway {
    @JsonProperty(value = "outcome", required = true)
    private OutcomeEnum outcomeEnum;
    private String rrn;
    private String authorizationCode;
    @JsonProperty(value = "errorCode")
    private VposErrorCodeEnum vposErrorCodeEnum;
}
