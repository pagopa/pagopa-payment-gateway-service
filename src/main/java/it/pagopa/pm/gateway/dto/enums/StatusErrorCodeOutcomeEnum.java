package it.pagopa.pm.gateway.dto.enums;

import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import lombok.Getter;

public enum StatusErrorCodeOutcomeEnum {

    GENERIC_ERROR(1L, "Generic error"),
    TIMEOUT( 4L,"Timeout"),
    MISSING_FIELDS( 6L,"Missing mandatory fields");

    @Getter
    private final Long outcome;
    @Getter
    private final String status;

    StatusErrorCodeOutcomeEnum(Long outcome, String status) {
        this.outcome = outcome;
        this.status = status;
    }

    StatusErrorCodeOutcomeEnum getEnum(ExceptionsEnum exceptionsEnum) {
        switch (exceptionsEnum) {
            case TIMEOUT:
                return TIMEOUT;
            case MISSING_FIELDS:
                return MISSING_FIELDS;
            default:
                return GENERIC_ERROR;
        }

    }



}
