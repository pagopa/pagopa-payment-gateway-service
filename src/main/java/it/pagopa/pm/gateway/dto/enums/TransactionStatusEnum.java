package it.pagopa.pm.gateway.dto.enums;

import lombok.*;

public enum TransactionStatusEnum {

    TX_TO_BE_AUTHORIZED(0L, "Da autorizzare", false),
    TX_PROCESSING(1L, "In attesa", false),
    TX_PROCESSING_MOD1(2L, "In attesa mod1", false),
    TX_ACCEPTED(3L, "Confermato", true),
    TX_REFUSED(4L, "rifiutato", true),
    TX_WAIT_FOR_RESUME_XPAY(5L, "In attesa di Xpay", false),
    TX_ERROR(6L, "In errore", true),
    TX_RESUMING_XPAY(7L, "Ritornando da Xpay", false),
    TX_ACCEPTED_MOD1(8L, "Confermato mod1", true),
    TX_ACCEPTED_MOD2(9L, "Confermato mod2", true),
    TX_ERROR_FROM_PSP(10L, "rifiutato", true),
    TX_MISSING_CALLBACK_FROM_PSP(11L, "Missing callback from PSP", true),
    TX_DIFFERITO_MOD1(12L, "Pagamento preso in carico", true),
    TX_EXPIRED_3DS(13L, "3DS Scaduto", true),
    TX_ACCEPTED_NODO_TIMEOUT(14L, "Autorizzato", true), //Authorized with nodo Timeout

    //3DS2
    TX_WAIT_FOR_3DS2_ACS_METHOD(15L, "In attesa del metodo 3ds2", false),
    TX_WAIT_FOR_3DS2_ACS_CHALLENGE(16L, "In attesa della challenge 3ds2", false),
    TX_RESUME_3DS2_ACS_METHOD(17L, "Ritornando dal metodo 3ds2", false),
    TX_RESUME_3DS2_ACS_CHALLENGE(18L, "Ritornando dalla challenge 3ds2", false),
    TX_TO_BE_REVERTED(19L, "Transazione da stornare con batch", true),
    TX_REVERTED(20L, "Transazione stornata con batch", true),

    //BPAY
    TX_AUTHORIZED_BANCOMAT_PAY(21L, "Pagamento autorizzato da Bancomat Pay", true);

    @Getter
    private final Long id;
    @Getter
    private final String description;
    @Getter
    private final boolean finalStatus;

    TransactionStatusEnum(Long id, String description, boolean finalStatus) {
        this.id = id;
        this.description = description;
        this.finalStatus = finalStatus;
    }

}
