package it.pagopa.pm.gateway.dto.vpos;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CcHttpException extends ResponseStatusException {
    public CcHttpException(HttpStatus statusCode, String message) {
        super(statusCode, message);
    }
}
