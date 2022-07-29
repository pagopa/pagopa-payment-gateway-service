package it.pagopa.pm.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PostePayOnboardingRequest {

    @JsonProperty(required = true)
    private String onboardingTransactionId;

}
