package it.pagopa.pm.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RestApiInternalException extends RestApiException{
    private static final long serialVersionUID = -8253284806525989657L;

    public RestApiInternalException(Integer errorCode, String message) {
        super(errorCode, message);
    }

}
