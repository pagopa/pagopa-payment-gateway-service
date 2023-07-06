package it.pagopa.pm.gateway.dto.xpay;

import lombok.*;

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
