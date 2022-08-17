package it.pagopa.pm.gateway.dto.enums;

import lombok.Getter;

public enum PaymentRequestStatusEnum {

    INITIALIZED(0L, "Inizializzata"),
    CREATED (1L, "Creata"),
    PROCESSED (2L, "Processata"),
    REFUNDED (3L, "Stornata"),
    NOT_REFUNDABLE (4L,"Non stornabile"),
    CANCELLED_BY_BATCH (5L, "Cancellata dal batch");


    @Getter
    private final Long id;
    @Getter
    private final String description;


    PaymentRequestStatusEnum(Long id, String description) {
        this.id = id;
        this.description = description;
    }

}
