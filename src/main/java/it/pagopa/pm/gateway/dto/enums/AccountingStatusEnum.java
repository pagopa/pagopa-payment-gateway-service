package it.pagopa.pm.gateway.dto.enums;

import lombok.*;

public enum AccountingStatusEnum {

    NOT_MANAGED(0L, "Non gestito"),
    ACCOUNTED(1L, "Contabilizzato"),
    ACCOUNT_ERROR(2L, "Errore di contabilizzazione"),
    REVERTED(3L, "Stornato"),
    REVERT_ERROR(4L, "Errore Storno");

    @Getter
    private final Long id;
    @Getter
    private final String description;

    AccountingStatusEnum(Long id, String description) {
        this.id = id;
        this.description = description;
    }

}
