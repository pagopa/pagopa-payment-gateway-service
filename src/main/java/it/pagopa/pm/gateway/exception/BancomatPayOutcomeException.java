package it.pagopa.pm.gateway.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
public class BancomatPayOutcomeException extends RestApiException{

    private static final long serialVersionUID = -8253284806525989657L;

    public BancomatPayOutcomeException(Integer errorCode, String message) {
        super(errorCode, message);

    }
}
