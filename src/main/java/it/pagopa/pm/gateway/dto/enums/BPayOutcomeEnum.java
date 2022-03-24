package it.pagopa.pm.gateway.dto.enums;

import lombok.*;

@AllArgsConstructor
@Getter
public enum BPayOutcomeEnum {

    OK("0", "Esito positivo"),
    PAYMENT_NOT_FOUND( "01017", "Pagamento non trovato"),
    GENERIC_ERROR( "-1", "Errore generico"),
    NO_ACTIVE_CONTACT_FOUND( "-2", "Non è stato trovato un contatto attivo associato al buyer selezionato"),
    NO_ACTIVE_BUYER_FOUND( "-3", "Nessun buyer attivo trovato"),
    BPAY_NOT_ACTIVE( "-4", "Il buyer non ha un servizio BANCOMAT Pay attivo"),
    NO_STORE_FOUND( "-5", "Nessun negozio online attivo trovato per il merchant"),
    NO_SERVICE_FOUND( "-6", "Il merchant non ha un servizio attivo"),
    BANK_NOT_ENABLED( "-89", "Operazione chiusa con esito negativo. Numero attivo su Banca non abilitata alle funzionalità di pagamento"),
    MERCHANT_NOT_FOUND( "-1002", "Merchant non trovato"),
    INVALID_AMOUNT( "01024", "L'importo del pagamento non è valido"),
    MERCHANT_BLOCKED( "01039", "Merchant bloccato per i pagamenti dello specifico buyer"),
    IDPAGOPA_OR_IDPSP_ALREADY_CREATED( "01065", "Errore l'idPagoPa o l'idPSP sono già censiti"),
    EMPTY_FIELD( "02000", "Il campo {0} non e' valorizzato"),
    MERCHANT_NOT_ENABLED( "03004", "Merchant non abilitato alla categoria di pagamento"),
    OPERATION_NOT_ALLOWED( "03016", "Operazione non consentita"),
    SERVICE_NOT_ENABLED( "03020", "Il servizio del negozio non è abilitato a censire questo pagamento");

    private final String code;
    private final String description;

    public static BPayOutcomeEnum fromCode(String code) {
        BPayOutcomeEnum[] values = BPayOutcomeEnum.values();
        for (BPayOutcomeEnum item : values) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Cannot find code " + code);
    }
    
}
