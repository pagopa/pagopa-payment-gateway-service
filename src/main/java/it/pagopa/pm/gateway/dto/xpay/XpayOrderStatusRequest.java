package it.pagopa.pm.gateway.dto.xpay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XpayOrderStatusRequest {

    private String apiKey;
    private String codiceTransazione;
    private String timeStamp;
    private String mac;

}
