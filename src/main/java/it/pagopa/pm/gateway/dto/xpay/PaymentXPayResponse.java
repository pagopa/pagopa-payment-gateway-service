package it.pagopa.pm.gateway.dto.xpay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentXPayResponse {

    private Long timeStamp;
    private EsitoXpay esito;
    private String idOperazione;
    private String codiceAutorizzazione;
    private String codiceConvenzione;
    private String data;
    private String nazione;
    private String regione;
    private String brand;
    private String tipoProdotto;
    private String tipoTransazione;
    private String mac;
    private XpayError errore;
}
