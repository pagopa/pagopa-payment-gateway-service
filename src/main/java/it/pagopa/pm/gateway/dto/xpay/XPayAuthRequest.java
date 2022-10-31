package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XPayAuthRequest {

    @JsonProperty(required = true)
    private String cvv;
    @JsonProperty(required = true)
    private String pan;
    @JsonProperty(required = true)
    private String expiryDate; //formato (yyyyMM)
    @JsonProperty(required = true)
    private BigInteger grandTotal;
    @JsonProperty(required = true)
    private String idTransaction;
}
