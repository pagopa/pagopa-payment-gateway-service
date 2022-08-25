package it.pagopa.pm.gateway.dto.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum PaymentRequestStatusEnum {

    CREATED (1L, "Creata"),
    AUTHORIZED (2L, "Autorizzazione concessa"),
    DENIED (3L, "Autorizzazione negata"),
    CANCELLED(4L, "Cancellata");

    private static final Map<String, PaymentRequestStatusEnum> map = new HashMap<>(values().length, 1);

    static {
        for (PaymentRequestStatusEnum c : values()) map.put(c.name(), c);
    }

    @Getter
    private final Long id;
    @Getter
    private final String description;

    PaymentRequestStatusEnum(Long id, String description) {
        this.id = id;
        this.description = description;
    }

    public static PaymentRequestStatusEnum of(String name) {
        PaymentRequestStatusEnum result = map.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Invalid status: " + name);
        }
        return result;
    }

}
