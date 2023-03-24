package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.pm.gateway.config.UnmaskSerializer;
import it.pagopa.pm.gateway.utils.GenericUtils;
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
    @JsonSerialize(using = UnmaskSerializer.class)
    private String cvv;

    @NotBlank
    @JsonProperty(required = true)
    @JsonSerialize(using = UnmaskSerializer.class)
    private String pan;

    @NotBlank
    @JsonProperty(required = true)
    @JsonSerialize(using = UnmaskSerializer.class)
    private String expiryDate; //formato (yyyyMM)

    @NotNull
    @JsonProperty(required = true)
    private BigInteger grandTotal;

    @NotBlank
    @JsonProperty(required = true)
    private String idTransaction;

    public String unmaskedCvv() {
        return cvv;
    }

    public String unmaskedPan() {
        return pan;
    }

    public String unmaskedExpiryDate() {
        return expiryDate;
    }

    private String getCvv() {
        return GenericUtils.maskValue(cvv);
    }

    private String getPan() {
        return GenericUtils.maskValue(pan);
    }

    private String getExpiryDate() {
        return GenericUtils.maskValue(expiryDate);
    }
}
