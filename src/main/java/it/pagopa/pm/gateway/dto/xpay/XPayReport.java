package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.pm.gateway.config.UnmaskSerializer;
import it.pagopa.pm.gateway.utils.GenericUtils;
import lombok.Data;

import java.util.List;

@Data
public class XPayReport {

    private String codiceAutorizzazione;
    private String codiceTransazione;
    private String dataTransazione;
    private Object mail;
    private String divisa;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String scadenza;
    private String importo;
    private String numeroMerchant;
    private String tipoTransazione;
    private String parametri;
    private String stato;
    private List<XPayDetails> dettaglio;
    private String tipoProdotto;
    private String nazione;
    private String numOrdinePm;
    private String tipoPagamento;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String pan;
    private String brand;

    public String unmaskedScadenza() {
        return scadenza;
    }

    public String unmaskedPan() {
        return pan;
    }

    private String getScadenza() {
        return GenericUtils.maskValue(scadenza);
    }

    private String getPan() {
        return GenericUtils.maskValue(pan);
    }

}
