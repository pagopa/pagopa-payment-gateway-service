package it.pagopa.pm.gateway.dto.bancomatpay;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BPayInfoResponse {

    private String abi;
    private String errorMessage;

}
