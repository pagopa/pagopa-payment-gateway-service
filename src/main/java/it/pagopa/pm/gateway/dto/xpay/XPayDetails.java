package it.pagopa.pm.gateway.dto.xpay;

import lombok.Data;

import java.util.List;

@Data
public class XPayDetails {

    private String codiceTransazione;
    private String mail;
    private String cognome;
    private String divisa;
    private Integer decimaliValuta;
    private String codiceValuta;
    private String importo;
    private String motivoEsito;
    private String controvaloreValuta;
    private String nome;
    private String parametriAggiuntivi;
    private String flagValuta;
    private String stato;
    private String tassoCambio;
    private String importoRifiutato;
    private List<XPayOperazione> operazioni;

}
