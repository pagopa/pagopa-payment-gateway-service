package it.pagopa.pm.gateway.dto.xpay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthPaymentXPayRequest {

    private String apiKey;
    private String pan;
    private String scadenza;
    private String cvv;
    private BigInteger importo;
    private String divisa;
    private String codiceTransazione;
    private String urlRisposta;
    private String timeStamp;
    private String mac;
}
