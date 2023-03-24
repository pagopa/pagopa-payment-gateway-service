package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.pm.gateway.config.UnmaskSerializer;
import it.pagopa.pm.gateway.utils.GenericUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthPaymentXPayRequest {

    private String apiKey;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String pan;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String scadenza;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String cvv;
    private BigInteger importo;
    private String divisa;
    private String codiceTransazione;
    private String urlRisposta;
    private String timeStamp;
    private String mac;
    private String mail;

    public String unmaskedPan() {
        return pan;
    }

    public String unmaskedScadenza() {
        return scadenza;
    }

    public String unmaskedCvv() {
        return cvv;
    }

    private String getPan() {
        return GenericUtils.maskValue(pan);
    }

    private String getScadenza() {
        return GenericUtils.maskValue(scadenza);
    }

    private String getCvv() {
        return GenericUtils.maskValue(cvv);
    }
}
