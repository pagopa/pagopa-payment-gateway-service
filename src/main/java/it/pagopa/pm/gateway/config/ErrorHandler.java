package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.exception.*;
import lombok.extern.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;

import javax.validation.*;
import java.util.*;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponse> handleRestApiException(RestApiException e) {
        log.error(e.getExceptionsEnum().getDescription());
        HttpStatus status = e.getExceptionsEnum().getRestApiCode();
        ErrorResponse errorResponse = new ErrorResponse();
        if (status.equals(HttpStatus.FAILED_DEPENDENCY)) {
            errorResponse.setCode(e.getCode());
        }
        switch (status) {
            case UNAUTHORIZED:
                errorResponse.setMessage("The X-Correlation-ID is not valid");
                break;
            case NOT_FOUND:
                errorResponse.setMessage("The X-Correlation-ID is unknown");
                break;
            case INTERNAL_SERVER_ERROR:
                errorResponse.setMessage(Arrays.toString(e.getStackTrace()));
                break;
            default:
                break;
        }
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class, ValidationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ve) {
        log.error(ve.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(null, ve.getMessage()));
    }

}
