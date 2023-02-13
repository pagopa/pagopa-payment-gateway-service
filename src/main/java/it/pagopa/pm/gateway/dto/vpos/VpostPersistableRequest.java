package it.pagopa.pm.gateway.dto.vpos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;

@Data
@AllArgsConstructor
public class VpostPersistableRequest {
    private String idTransaction;
    private BigInteger amount;
    private Boolean isFirstPayment;
    private String idPsp;
}
