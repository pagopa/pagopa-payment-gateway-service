package it.pagopa.pm.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RestApiInternalException extends RestApiException{

    public RestApiInternalException(Integer errorCode, String message) {
        super(errorCode, message);
    }

}
