package it.pagopa.pm.gateway.dto.creditcard;

import it.pagopa.pm.gateway.dto.enums.CardCircuit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StepZeroRequest {
    private String idTransaction;
    private String reqRefNumber;
    private BigInteger amount;
    private String pan;
    private String securityCode;
    private String expireDate;
    private String holder;
    private CardCircuit circuit;
    private String threeDsData;
    private String emailCH;
    private Boolean isFirstPayment;
    private String idPsp;
}
