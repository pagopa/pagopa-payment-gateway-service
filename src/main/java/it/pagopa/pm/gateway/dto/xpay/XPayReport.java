package it.pagopa.pm.gateway.dto.xpay;

import lombok.Data;

import java.util.List;

@Data
public class XPayReport {

    private String codiceAutorizzazione;
    private String codiceTransazione;
    private String dataTransazione;
    private Object mail;
    private String divisa;
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
    private String pan;
    private String brand;

}
