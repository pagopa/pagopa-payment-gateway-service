package it.pagopa.pm.gateway.dto.postepay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostePayOnboardingRequest {

    @JsonProperty(required = true)
    private String onboardingTransactionId;

}
