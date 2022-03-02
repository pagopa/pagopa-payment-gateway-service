package it.pagopa.pm.gateway.exception;

import lombok.*;

public class RestApiException extends Exception {

    @Getter
    private final ExceptionsEnum exceptionsEnum;

    @Getter
    private Integer code = null;

    public RestApiException(ExceptionsEnum exceptionsEnum) {
        super();
        this.exceptionsEnum = exceptionsEnum;
    }

    public RestApiException(ExceptionsEnum exceptionsEnum, Integer code) {
        super();
        this.exceptionsEnum = exceptionsEnum;
        this.code = code;
    }

}
