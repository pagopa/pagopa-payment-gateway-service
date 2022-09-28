package it.pagopa.pm.gateway.dto.xpay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthPaymentXPayResponse {

    private EsitoXpay esito;
    private String idOperazione;
    private Long timeStamp;
    private String html;
    private XpayError errore;
    private String mac;

}
