package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;

@Data
public class Shop {

    /**
     * A VPos shop is identified by the following parameters:
     * idPsp, ABI, shopId, terminalId, MAC
     * where shopId, terminalId and MAC are different for first or subsequent payments.
     */

    private String idPsp;
    private String abi;
    private String shopIdFirstPayment;
    private String terminalIdFirstPayment;
    private String macFirstPayment;
    private String shopIdSuccPayment;
    private String terminalIdSuccPayment;
    private String macSuccPayment;
}
