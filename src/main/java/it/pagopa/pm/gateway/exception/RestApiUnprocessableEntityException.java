package it.pagopa.pm.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class RestApiUnprocessableEntityException extends RestApiException {
    private static final long serialVersionUID = -1378278078164043359L;

    public RestApiUnprocessableEntityException(Integer errorCode, String message) {
        super(errorCode, message);

}

}
