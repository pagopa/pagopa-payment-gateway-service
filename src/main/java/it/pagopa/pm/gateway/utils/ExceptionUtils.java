package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.exception.*;
import lombok.extern.log4j.Log4j;

@Log4j
public class ExceptionUtils {

    public static void handleRestException(Exception e) throws RestApiException {
        if (e instanceof BusinessException) {
            log.warn("BusinessException: " + e);
            throw new RestApiUnprocessableEntityException(((BusinessException) e).getException().getRestApiCode(), e.getMessage());
        } else if (e instanceof BancomatPayClientException ){


        }
        else {
            log.warn("Exception: ", e);
            throw new RestApiInternalException(ExceptionsEnum.GENERIC_ERROR.getRestApiCode(), ExceptionsEnum.GENERIC_ERROR.getDescription());
        }
    }
}
