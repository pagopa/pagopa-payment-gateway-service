package it.pagopa.pm.gateway.dto;

import it.pagopa.pm.gateway.dto.xpay.EsitoXpay;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XPayResumeRequest {

    private EsitoXpay esito;
    private String idOperazione;
    private String xpayNonce;
    private String timestamp;
    private String mac;
    private String codice;
    private String messaggio;

}
