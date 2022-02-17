package it.pagopa.pm.gateway.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class BancomatPayClientException extends RestApiException{
    private static final long serialVersionUID = -8253284806525989657L;

    public BancomatPayClientException(Integer errorCode, String message) {
        super(errorCode, message);

    }

}