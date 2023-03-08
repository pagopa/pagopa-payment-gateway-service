package it.pagopa.pm.gateway.dto.creditcard;

import it.pagopa.pm.gateway.dto.enums.CardCircuit;
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
    private String pan;

    @NotBlank
    private String securityCode;

    @NotBlank
    private String expireDate;

    @NotBlank
    private String holder;

    @NotNull
    private CardCircuit circuit;

    @NotBlank
    private String threeDsData;

    @NotBlank
    private String emailCH;

    @NotNull
    private Boolean isFirstPayment;

    @NotBlank
    private String idPsp;
}
