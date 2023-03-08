package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XPayAuthRequest {

    @NotBlank
    @JsonProperty(required = true)
    private String cvv;

    @NotBlank
    @JsonProperty(required = true)
    private String pan;

    @NotBlank
    @JsonProperty(required = true)
    private String expiryDate; //formato (yyyyMM)

    @NotNull
    @JsonProperty(required = true)
    private BigInteger grandTotal;

    @NotBlank
    @JsonProperty(required = true)
    private String idTransaction;
}
