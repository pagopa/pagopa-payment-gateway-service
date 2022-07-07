package it.pagopa.pm.gateway.dto.enums;

import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import lombok.Getter;

public enum StatusErrorCodeOutcomeEnum {

    GENERIC_ERROR(1L, "Generic error"),
    TIMEOUT( 4L,"Timeout"),
    MISSING_FIELDS( 6L,"Missing fields"),
    NOT_FOUND( 11L,"404 Not Found");

    @Getter
    private final Long outcome;
    @Getter
    private final String status;

    StatusErrorCodeOutcomeEnum(Long outcome, String status) {
        this.outcome = outcome;
        this.status = status;
    }

    public static StatusErrorCodeOutcomeEnum getEnum(ExceptionsEnum exceptionsEnum) {
        switch (exceptionsEnum) {
            case TIMEOUT:
                return TIMEOUT;
            case MISSING_FIELDS:
                return MISSING_FIELDS;
            case TRANSACTION_NOT_FOUND:
                return NOT_FOUND;
            default:
                return GENERIC_ERROR;
        }

    }



}
