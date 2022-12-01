package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;

@Data
public class Shop {

    private String idPsp;
    private String abi;
    private String shopIdFirstPayment;
    private String terminalIdFirstPayment;
    private String macFirstPayment;
    private String shopIdSuccPayment;
    private String terminalIdSuccPayment;
    private String macSuccPayment;
}
