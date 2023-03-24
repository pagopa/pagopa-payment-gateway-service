package it.pagopa.pm.gateway.dto.creditcard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.pm.gateway.config.UnmaskSerializer;
import it.pagopa.pm.gateway.dto.enums.CardCircuit;
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
public class StepZeroRequest {
    @NotBlank
    private String idTransaction;

    @NotNull
    private BigInteger amount;

    @NotBlank
    @JsonSerialize(using = UnmaskSerializer.class)
    private String pan;

    @NotBlank
    @JsonSerialize(using = UnmaskSerializer.class)
    private String securityCode;

    @NotBlank
    @JsonSerialize(using = UnmaskSerializer.class)
    private String expireDate;

    @NotBlank
    @JsonSerialize(using = UnmaskSerializer.class)
    private String holder;

    @NotNull
    private CardCircuit circuit;

    @NotBlank
    private String threeDsData;

    @NotBlank
    @JsonSerialize(using = UnmaskSerializer.class)
    private String emailCH;

    @NotNull
    private Boolean isFirstPayment;

    @NotBlank
    private String idPsp;

    public String unmaskedPan() {
        return pan;
    }

    public String unmaskedHolder() {
        return holder;
    }

    public String unmaskedExpireDate() {
        return expireDate;
    }

    public String unmaskedSecurityCode() {
        return securityCode;
    }

    public String unmaskedEmailCH() {
        return emailCH;
    }

    private String getPan() {
        return GenericUtils.maskValue(this.pan);
    }

    private String getHolder() {
        return GenericUtils.maskValue(this.holder);
    }

    private String getExpireDate() {
        return GenericUtils.maskValue(this.expireDate);
    }

    private String getSecurityCode() {
        return GenericUtils.maskValue(this.securityCode);
    }

    private String getEmailCH() {
        return GenericUtils.maskValue(this.emailCH);
    }
}
