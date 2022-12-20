package it.pagopa.pm.gateway.dto.creditcard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditCardResumeRequest {

    private String methodCompleted;
}
