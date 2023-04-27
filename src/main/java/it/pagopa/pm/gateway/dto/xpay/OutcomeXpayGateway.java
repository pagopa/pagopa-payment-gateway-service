package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@JsonInclude(NON_NULL)
public class OutcomeXpayGateway {
    @JsonProperty(value = "outcome", required = true)
    private OutcomeEnum outcomeEnum;
    private String authorizationCode;
    private String errorCode;
}
