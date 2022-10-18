package it.pagopa.pm.gateway.dto.xpay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XPayRevertResponse {

    private EsitoXpay esito;
    private String idOperazione;
    private Long timeStamp;
    private String mac;
    private XpayError errore;

}
