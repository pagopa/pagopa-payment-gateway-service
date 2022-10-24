package it.pagopa.pm.gateway.dto.xpay;

import lombok.Data;

@Data
public class XPayOperazione {

    private String tipoOperazione;
    private Long importo;
    private String divisa;
    private String stato;
    private String dataOperazione;
    private String utente;
    private String idContabParzialePayPal;

}
