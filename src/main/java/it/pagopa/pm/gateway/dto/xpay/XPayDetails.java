package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.pm.gateway.config.UnmaskSerializer;
import it.pagopa.pm.gateway.utils.GenericUtils;
import lombok.Data;

import java.util.List;

@Data
public class XPayDetails {

    private String codiceTransazione;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String mail;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String cognome;
    private String divisa;
    private Integer decimaliValuta;
    private String codiceValuta;
    private String importo;
    private String motivoEsito;
    private String controvaloreValuta;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String nome;
    private String parametriAggiuntivi;
    private String flagValuta;
    private String stato;
    private String tassoCambio;
    private String importoRifiutato;
    private List<XPayOperazione> operazioni;

    public String unmaskedMail() {
        return mail;
    }

    public String unmaskedCognome() {
        return cognome;
    }

    public String unmaskedNome() {
        return nome;
    }

    private String getMail() {
        return GenericUtils.maskValue(mail);
    }

    private String getCognome() {
        return GenericUtils.maskValue(cognome);
    }

    private String getNome() {
        return GenericUtils.maskValue(nome);
    }
}
