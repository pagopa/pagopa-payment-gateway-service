package it.pagopa.pm.gateway.exception;

import lombok.*;

public class RestApiException extends Exception {

    @Getter
    private final ExceptionsEnum exceptionsEnum;

    public RestApiException(ExceptionsEnum exceptionsEnum) {
        super();
        this.exceptionsEnum = exceptionsEnum;
    }

}
