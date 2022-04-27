package it.pagopa.pm.gateway.exception;

import lombok.*;
import org.springframework.http.*;

public enum ExceptionsEnum {

    GENERIC_ERROR("Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR),
    TRANSACTION_ALREADY_PROCESSED("Transaction already processed", HttpStatus.UNAUTHORIZED),
    RESTAPI_CD_CLIENT_ERROR ("Exception during call to RestapiCD", HttpStatus.FAILED_DEPENDENCY),
    TRANSACTION_NOT_FOUND("Transaction not found", HttpStatus.NOT_FOUND),
    TIMEOUT("Timeout", HttpStatus.GATEWAY_TIMEOUT),
    MISSING_IDPAGOPA_AND_CORRELATIONID("Missing both idPagoPa and CorrelationId in Request", HttpStatus.BAD_REQUEST);

    @Getter
    private final String description;
    @Getter
    private final HttpStatus restApiCode;

    ExceptionsEnum(String description, HttpStatus restApiCode) {
        this.description = description;
        this.restApiCode = restApiCode;
    }

}
