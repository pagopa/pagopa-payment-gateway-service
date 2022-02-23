package it.pagopa.pm.gateway.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
public class BancomatPayOutcomeException extends RestApiException{

    public BancomatPayOutcomeException(Integer errorCode, String message) {
        super(errorCode, message);

    }
}
