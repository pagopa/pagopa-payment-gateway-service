package it.pagopa.pm.gateway.dto.xpay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentXPayRequest {

    private String apiKey;
    private String codiceTransazione;
    private BigInteger importo;
    private Long divisa;
    private String xpayNonce;
    private String timeStamp;
    private String mac;

}
