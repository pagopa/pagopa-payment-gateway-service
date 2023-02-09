package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;

import java.math.BigInteger;

@Data
public class VposDatabaseRequest {
    private String idTransaction;
    private BigInteger amount;
    private Boolean isFirstPayment;
    private String idPsp;
}
