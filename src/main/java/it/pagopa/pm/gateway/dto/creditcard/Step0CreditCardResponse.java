package it.pagopa.pm.gateway.dto.creditcard;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Step0CreditCardResponse {

    private String urlRedirect;
    private String error;
    private String requestId;
    private String status;
}
